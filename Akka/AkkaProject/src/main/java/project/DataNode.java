package project;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.cluster.typed.Cluster;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DataNode {

    public interface Command extends CborSerializable{}

    //messages
    public static final class GetRequest implements Command {
        public final String key;
        public final ActorRef<Command> replyTo;

        public GetRequest( String key, ActorRef<Command> replyTo){
            this.key=key;
            this.replyTo=replyTo;
        }
    }

    public static final class GetAnswer implements Command{
        public final String key;
        public final String value;
        public final boolean isPresent;
        public final int requestId;

        public GetAnswer(String key, String value, boolean isPresent, Integer requestId){
            this.key = key;
            this.value = value;
            this.isPresent = isPresent;
            this.requestId = requestId;
        }


    }

    public static final class Get implements Command{
        public final String key;
        public final ActorRef<Command> replyTo;
        public final int requestId;
        public final int successorID;

        public Get(String key, ActorRef<Command> replyTo, int requestId, int successorID){
            this.key=key;
            this.replyTo=replyTo;
            this.requestId = requestId;
            this.successorID = successorID;
        }
    }
    //---------------------------------------------------------------------------------------------------

    public static final class PutRequest implements Command{
        public final String key;
        public final Value value;
        public final ActorRef<Command> replyTo;

        public PutRequest(String key, Value value, ActorRef<Command> replyTo){
            this.key=key;
            this.value=value;
            this.replyTo=replyTo;
        }
    }

    public static final class PutAnswer implements Command{
        public final boolean success;
        public final int requestId;

        public PutAnswer(boolean success, Integer requestId){
            this.success = success;
            this.requestId = requestId;
        }
    }

    public static final class Put implements Command{
        public final String key;
        public final Value value;
        public final ActorRef<Command> replyTo;
        public final boolean isReplica;
        public final int requestId;
        public final int successorId;

        public Put(String key, Value value, ActorRef<Command> replyTo, boolean isReplica, Integer requestId, int successorId) {
            this.key = key;
            this.value = value;
            this.replyTo = replyTo;
            this.isReplica = isReplica;
            this.requestId = requestId;
            this.successorId = successorId;
        }
    }

    public static class NodesUpdate implements Command{
        public final  Set<ActorRef<Command>> currentNodes;

        public NodesUpdate (Set<ActorRef<Command>> currentNodes){
            this.currentNodes=currentNodes;
        }
    }




    /*
    ----------------------------------------------------------------------
                                TEST MESSAGES
    ---------------------------------------------------------------------
     */

    public interface TestCommand extends Command{}

    public static class GetAllLocalRequest implements TestCommand {
        public final ActorRef<DataNode.Command> replyTo;

        public GetAllLocalRequest(ActorRef<DataNode.Command> replyTo){
            this.replyTo = replyTo;
        }
    }

    public static class GetAllLocalAnswer implements TestCommand {
        public final Collection<String> values;

        public GetAllLocalAnswer(Collection<String> values){ this.values = values;
        }
    }

    public static class GetNodesRequest implements TestCommand{
        public final ActorRef<DataNode.Command> replyTo;

        public GetNodesRequest(ActorRef<DataNode.Command> replyTo){ this.replyTo = replyTo;
        }
    }

    public static class GetNodesAnswer implements TestCommand{
        public final List<NodeInfo> nodes;

        public GetNodesAnswer(List<NodeInfo> nodes){ this.nodes = nodes;
        }
    }

    //-----------------------------------------------------------------------
    //static attributes
    private static final ServiceKey<Command> KEY= ServiceKey.create(Command.class, "node");
    private static final String IPADDRESSPATTERN = "(([0-9]{3})(\\.)([0-9]{3})(\\.)([0-9]{1,3})(\\.)([0-9]{1,3}))";
    private static final String PORTPATTERN = "([0-9]{5})";
    private static final String NODEPATTERN = "Actor[akka://ClusterSystem@" + IPADDRESSPATTERN + ":" + PORTPATTERN + "/user/DataNode#-" + "([0-9]+)]";
    private static final String IDENTIFIER = IPADDRESSPATTERN + ":" + PORTPATTERN;

    //final actor attributes
    private final String port;
    private final String address;
    private final int nReplicas;
    private final List<NodeInfo> nodes = new ArrayList<>();
    private final ActorContext<Command> context;
    private final HashMap<String,Value> data = new HashMap<>();
    private final HashMap<String,Value> replicas = new HashMap<>();
    private final Random loadBalancer = new Random();

    private final HashMap<Integer, Request> requests = new HashMap<>();
    private final List<Command> toPerform = new ArrayList<>();


    //non final actor attributes
    private int nodeId;
    private Integer ticket;

    //--------------------------------------------------------------------------------


    public static Behavior<Command> create(int nReplicas) {
        return Behaviors.setup(context -> {
            DataNode dataNode = new DataNode(context,nReplicas);
            context.getSystem().receptionist().tell(Receptionist.register(KEY, context.getSelf()));
            context.getLog().info("registering with the receptionist...");
            ActorRef<Receptionist.Listing> subscriptionAdapter =
                context.messageAdapter(Receptionist.Listing.class, listing ->
                    new NodesUpdate(listing.getServiceInstances(DataNode.KEY)));
            context.getLog().info("subscribing with the receptionist...");
            context.getSystem().receptionist().tell(Receptionist.subscribe(DataNode.KEY,subscriptionAdapter));
            return dataNode.behavior();
        });
    }

    //constructor
    private DataNode(ActorContext<Command> context,int nReplicas) {
        this.context = context;
        this.nReplicas = nReplicas;
        Cluster cluster = Cluster.get(context.getSystem());
        Optional<String> maybeAddress = cluster.selfMember().address().getHost();
        Optional<Integer> maybePort = cluster.selfMember().address().getPort();
        if (maybeAddress.isPresent() && maybePort.isPresent()){
            this.address = maybeAddress.get();
            this.port = String.valueOf(maybePort.get());
        } else {
            this.address = "127.0.0.1";
            this.port = String.valueOf(25521);
        }
        String hashKey = hashfunction(address,port);
        nodes.add(new NodeInfo(hashKey, context.getSelf()));
        this.nodeId = 0;
        this.ticket = 1;
    }

    //behaviour constructor
    private Behavior<Command> behavior() {
        return Behaviors.receive(Command.class)
                .onMessage(GetRequest.class, this::onGetRequest).
                        onMessage(GetAnswer.class, this::onGetAnswer).
                        onMessage(PutRequest.class, this::onPutRequest).
                        onMessage(PutAnswer.class, this::onPutAnswer).
                        onMessage(NodesUpdate.class, this:: onNodesUpdate).
                        onMessage(Put.class, this::onPut).
                        onMessage(GetAllLocalRequest.class, this::onGetAllLocalRequest).
                        onMessage(GetNodesRequest.class,this::onGetNodesRequest).
                        onMessage(Get.class,this::onGet).
                        build();
    }

    /*--------------------------------------
    GET BEHAVIOUR
     */


    private Behavior<Command> onGetRequest(GetRequest message){
        int nodePosition = message.key.hashCode() % nodes.size();
        if(nodePosition < 0 ) nodePosition += nodes.size();
        nodes.sort(Comparator.comparing(NodeInfo::getHashKey));
        //checking whether the cluster is big enough
        List<NodeInfo> successors = null;
        try{
            successors = getSuccessorNodes(nodePosition, nReplicas, nodes);
        }catch (ClusterException e){
            toPerform.add(message);
            return Behaviors.same();
        }
        List<String> successorsNames = successors
                .stream()
                .map(NodeInfo::getHashKey)
                .collect(Collectors.toList());
        if (nodePosition == this.nodeId || successorsNames.contains(hashfunction(address, port))){
            //I return the value I've stored, even if null, and I specify if it's present in the answer message
            Value value = this.data.get(message.key);
            boolean isPresent = value != null;
            message.replyTo
                    .tell(new GetAnswer(message.key,isPresent? value.value : null , isPresent,ticket));
        }
        else {
            //I contact a random node which is supposed to keep a replica
            int choice = loadBalancer.nextInt(nReplicas +1);
            if (choice == nReplicas){
                // I contact the leader
                ActorRef<Command> leader = nodes.get(nodePosition).getNode();
                leader.tell(new Get(message.key, context.getSelf(), ticket, choice));
            }
            else{
                successors
                        .get(choice)
                        .getNode()
                        .tell(new Get(message.key, context.getSelf(), ticket,choice));
            }
            requests.put(ticket, new Request(1, message.replyTo));

        }
        ticket++;
        return Behaviors.same();
    }

    private Behavior<Command> onGet(Get message){
        //check if the topology has changed in the meantime
        int nodePosition = message.key.hashCode() % nodes.size();
        if(nodePosition < 0 ) nodePosition += nodes.size();
        nodes.sort(Comparator.comparing(NodeInfo::getHashKey));
        //checking whether the cluster is big enough
        List<NodeInfo> successors = null;
        try{
            successors = getSuccessorNodes(nodePosition, nReplicas, nodes);
        }catch (ClusterException e){
            toPerform.add(message);
            return Behaviors.same();
        }
        List<String> successorsNames = successors
                .stream()
                .map(NodeInfo::getHashKey)
                .collect(Collectors.toList());
        if (message.successorID == nReplicas && nodePosition!= this.nodeId){
            ActorRef<Command> leader = nodes.get(nodePosition).getNode();
            leader.tell(new Get(message.key, message.replyTo, message.requestId, message.successorID));
            return Behaviors.same();
        }
        if( !(successorsNames.get(message.successorID).equals(hashfunction(address,port)))) {
            // I redirect the request to the real destination
            successors
                    .get(message.successorID)
                    .getNode()
                    .tell(new Get(message.key, message.replyTo, message.requestId, message.successorID));
            return Behaviors.same();
        }
        //if no changes, reply
        Value value = this.data.get(message.key);
        boolean isPresent = value != null;
        message.replyTo.tell(new GetAnswer(message.key, isPresent? value.value : null, isPresent,message.requestId));
        return Behaviors.same();
    }

    private Behavior<Command> onGetAnswer(GetAnswer message){
        if (requests.containsKey(message.requestId)){
            Request request = requests.remove(message.requestId);
            request.setCounter(request.getCounter()-1);
            if (request.getCounter() == 0) request.requester
                    .tell(new GetAnswer(message.key, message.value,message.isPresent, message.requestId));
            else{
                requests.put(message.requestId, request);
            }
        }
        //otherwise just drop the message
        return Behaviors.same();
    }


    /*---------------------------------------------------------------
    PUT BEHAVIOUR
     */

    private Behavior<Command> onPutRequest(PutRequest message){
        int nodePosition = message.key.hashCode() % nodes.size();
        if(nodePosition < 0 ) nodePosition += nodes.size();
        nodes.sort(Comparator.comparing(NodeInfo::getHashKey));
        //checking whether the cluster is big enough
        List<NodeInfo> successors = null;
        try{
            successors = getSuccessorNodes(nodePosition, nReplicas, nodes);
        }catch (ClusterException e){
            toPerform.add(message);
            return Behaviors.same();
        }
        if (nodePosition == this.nodeId){
            //I'm the leader, so I add the value to my data
            if (message.value.version == -1){
                int version = 0;
                if (this.data.containsKey(message.key)) version = this.data.get(message.key).version +1;
                message.value.version = version;
            }
            this.data.put(message.key,message.value);
            context.getLog().info("just inserted a leader version of key-data "+ message.key + " " + message.value.value +  " ...");
            for (int i = 0; i< nReplicas; i++){
                successors.get(i).getNode().tell(new Put(message.key,message.value, context.getSelf(), true, ticket,i));
            }
            Request request = new Request(nReplicas, message.replyTo);
            requests.put(ticket, request);
        }else{
            //I send the data to the leader of that data, and wait for a reply
            NodeInfo node = nodes.get(nodePosition);
            node.getNode().tell(new Put(message.key,message.value, context.getSelf(),false, ticket, nodes.size()));
            Request request = new Request(1, message.replyTo);
            requests.put(ticket, request);
        }
        ticket++;
        return Behaviors.same();
    }

    private Behavior<Command> onPutAnswer(PutAnswer message){
        if (requests.containsKey(message.requestId)){
            Request request = requests.remove(message.requestId);
            request.setCounter(request.getCounter()-1);
            if (request.getCounter() == 0) request.requester.tell(new PutAnswer(true, message.requestId));
            else{
                requests.put(message.requestId, request);
            }
        }
        return Behaviors.same();
    }

    private Behavior<Command> onPut(Put message){
        //recomputing the leader in case the topology has changed in the meantime
        int nodePosition = message.key.hashCode() % nodes.size();
        if(nodePosition < 0 ) nodePosition += nodes.size();
        nodes.sort(Comparator.comparing(NodeInfo::getHashKey));
        //checking whether the cluster is big enough
        List<NodeInfo> successors = null;
        try{
            successors = getSuccessorNodes(nodePosition, nReplicas, nodes);
        }catch (ClusterException e){
            toPerform.add(message);
            return Behaviors.same();
        }
        List<String> successorsNames = successors
                .stream()
                .map(NodeInfo::getHashKey)
                .collect(Collectors.toList());

        if ((message.isReplica && !successorsNames.get(message.successorId).equals(hashfunction(address,port)))){
            context.getLog().info("redirecting put to true replica, current size " + this.nodes.size() + "...");
            successors
                    .get(message.successorId)
                    .getNode()
                    .tell(new Put(message.key,message.value, message.replyTo, true, message.requestId, message.successorId));
            return Behaviors.same();
        }

        if (!message.isReplica && nodePosition != this.nodeId){
            NodeInfo node = nodes.get(nodePosition);
            context.getLog().info("redirecting put to true leader, current size " + this.nodes.size() + "...");
            node.getNode().tell(new Put(message.key,message.value, message.replyTo, false, message.requestId, nReplicas));
            return Behaviors.same();
        }

        if ( message.isReplica){
            //checking the version number
            if (this.replicas.containsKey(message.key)){
                if (this.replicas.get(message.key).version >= message.value.version){
                    message.replyTo.tell(new PutAnswer(true, message.requestId));
                    return Behaviors.same();
                }
            }
            this.replicas.put(message.key, message.value);
            context.getLog().info("just inserted a replica of key-data "+ message.key + " " + message.value.value +  " ...");
            message.replyTo.tell(new PutAnswer(true, message.requestId));
        }
        else{
            // assigning the correct version number in case it hasn't been assigned
            if (message.value.version == -1){
                int version = 0;
                if (this.data.containsKey(message.key)) version = this.data.get(message.key).version +1;
                message.value.version = version;
            }

            //checking the version number
            if (this.data.containsKey(message.key)){
                if (this.data.get(message.key).version >= message.value.version){
                    message.replyTo.tell(new PutAnswer(true, message.requestId));
                    return  Behaviors.same();
                }
            }

            //inserting the copy
            this.data.put(message.key,message.value);
            context.getLog().info("just inserted a leader version of key-data "+ message.key + " " + message.value.value +  " ...");
            //inform the replicas
            for (int i = 0; i< nReplicas; i++){
                successors.get(i).getNode().tell(new Put(message.key,message.value, context.getSelf(), true, ticket,i));
            }
            Request request = new Request(nReplicas, message.replyTo);
            requests.put(ticket, request);
            ticket++;
        }
        return Behaviors.same();
    }


    private Behavior<Command> onNodesUpdate(NodesUpdate message) {
        context.getLog().info("The cluster has changed");
        this.nodes.clear();
        //update the table of all the nodes
        for (ActorRef<Command> node: message.currentNodes){
            Pattern pattern = Pattern.compile(IDENTIFIER);
            Matcher matcher = pattern.matcher(node.toString());
            if (matcher.find()){
                String identifier = matcher.group(0);
                String[] splittedIdentifier = identifier.split(":");
                String hashKey = hashfunction(splittedIdentifier[0], splittedIdentifier[1]);
                this.nodes.add(new NodeInfo(hashKey, node));
            }
        }

        //add this node to the table
        String hashKey = hashfunction(address,port);
        nodes.add(new NodeInfo(hashKey, context.getSelf()));

        //recomputing the ID of this node in the cluster
        nodes.sort(Comparator.comparing(NodeInfo::getHashKey));
        int i =0;
        for (NodeInfo node: nodes){
            if (node.getNode().toString().equals(context.getSelf().toString())){
                nodeId = i;
            }
            i++;
        }

        //performing messages not sent due to cluster not big enough and reassigning all values
        if ( nodes.size() >= nReplicas +1){
            for( Command command: this.toPerform){
                context.getSelf().tell(command);
            }
        }

        //reassigning all the values
        HashMap<String,Value> allData = new HashMap<>(this.data);
        allData.putAll(this.replicas);
        this.data.clear();
        this.replicas.clear();
        allData.entrySet()
            .forEach(entry -> {
                int nodePosition = entry.getKey().hashCode() % nodes.size();
                if(nodePosition < 0 ) nodePosition += nodes.size();
                nodes.sort(Comparator.comparing(NodeInfo::getHashKey));
                //checking whether the cluster is big enough
                List<NodeInfo> successors = null;
                try{
                    successors = getSuccessorNodes(nodePosition, nReplicas, nodes);
                }catch (ClusterException e){
                    //this exception is never thrown here due to if clause
                }
                if (nodePosition == this.nodeId){
                    context.getLog().info("just inserted a leader version of key-data "+ entry.getKey() + " " + entry.getValue().value +  " due to new topology...");
                    //I'm the leader, so I add the value to my data
                    this.data.put(entry.getKey(),entry.getValue());
                }else{
                    //I send the data to the leader of that data
                    context.getLog().info("sending an update to the leader of this data: I'm " + this.port + "..." );
                    ActorRef<Command> node = nodes.get(nodePosition).getNode();
                    node.tell(new Put(entry.getKey(), entry.getValue(), context.getSelf(), false, ticket, nReplicas));

                }
                //optimizations are possible here
                boolean isReplica = successors.stream().map(NodeInfo::getHashKey).collect(Collectors.toList()).contains(hashfunction(address,port));
                if (isReplica) this.replicas.put(entry.getKey(), entry.getValue());
                for ( int k=0; k<nReplicas; k++){
                    successors.get(k).getNode().tell(new Put(entry.getKey(), entry.getValue(), context.getSelf(), true, ticket,k));
                }
                ticket++;
            });
        return Behaviors.same();
    }




    //----------------------------------------------------------------------------------
    //supporting functions
    //converting ip port -> hash
    private static String hashfunction(String address, String port){
        String key = address + ":" + port;
        MessageDigest digest;
        byte[] hash;
        StringBuffer hexHash = new StringBuffer();
        try {
            // Create the SHA-1 of the nodeidentifier
            digest = MessageDigest.getInstance("SHA-1");
            hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            // Convert hash bytes into StringBuffer ( via integer representation)
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexHash.append('0');
                hexHash.append(hex);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hexHash.toString();
    }

    private List<NodeInfo> getSuccessorNodes(int nodeId, int nReplicas, List<NodeInfo> nodes)throws ClusterException{
        int nNodes = nodes.size();
        List<NodeInfo> selectedNodes = new LinkedList<>();
        int i=1;
        while(i <= nReplicas && nodeId + i < nNodes){
            NodeInfo node = nodes.get(nodeId+i);
            selectedNodes.add(node);
            i++;
        }
        int j=0;
        while (i <= nReplicas && j < nodeId){
            NodeInfo node = nodes.get(j);
            selectedNodes.add(node);
            i++;
            j++;
        }
        if (selectedNodes.size() < nReplicas) throw new ClusterException();
        return selectedNodes;
    }


    @Override
    public String toString(){
        return address + ":" + port;
    }

    @Override
    public boolean equals (Object o){
        if (o instanceof DataNode){
            DataNode toCompare = (DataNode) o;
            return  this.toString().equals(toCompare.toString());
        }
        return false;
    }

    @Override
    public int hashCode(){
        return Objects.hash(this.toString());
    }
    /*
    ------------------------------------------------------------------------------------------------
    TEST MESSAGES
    ---------------------------------------------------------------------------------------------

     */

    private Behavior<Command> onGetAllLocalRequest(GetAllLocalRequest message){
        Collection<String> allData = new ArrayList<>(this.data.values().stream().map(n -> n.value).collect(Collectors.toList()));
        context.getLog().info(allData.size() + " number of leader data");
        Collection<String> replicas = new ArrayList<>(this.replicas.values().stream().map(n -> n.value).collect(Collectors.toList()));
        context.getLog().info(replicas.size() + " number of replica data");
        allData.addAll(replicas);
        message.replyTo.tell(new GetAllLocalAnswer(allData));
        return Behaviors.same();
    }

    private Behavior<Command> onGetNodesRequest (GetNodesRequest message){
        message.replyTo.tell(new GetNodesAnswer(this.nodes));
        return Behaviors.same();
    }
}
