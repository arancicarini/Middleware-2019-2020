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

        public Get(String key, ActorRef<Command> replyTo, int requestId){
            this.key=key;
            this.replyTo=replyTo;
            this.requestId = requestId;
        }
    }
    //---------------------------------------------------------------------------------------------------

    public static final class PutRequest implements Command{
        public final String key;
        public final String value;
        public final ActorRef<Command> replyTo;

        public PutRequest(String key, String value, ActorRef<Command> replyTo){
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
        public final String value;
        public final ActorRef<Command> replyTo;
        public final boolean isReplica;
        public final int requestId;

        public Put(String key, String value, ActorRef<Command> replyTo, boolean isReplica, Integer requestId) {
            this.key = key;
            this.value = value;
            this.replyTo = replyTo;
            this.isReplica = isReplica;
            this.requestId = requestId;
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
    private final HashMap<String,String> data = new HashMap<>();
    private final HashMap<String,String> replicas = new HashMap<>();
    private final HashMap<Integer, ActorRef<Command>> requests = new HashMap<>();

    //non final actor attributes
    private int nodeId;
    private int ticket;

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
        System.out.println(nodes.size() + " this is the size of the cluster");
        System.out.println(nodePosition + "   NODE POSITION for key " + message.key);


        nodes.sort(Comparator.comparing(NodeInfo::getHashKey));
        if (nodePosition == this.nodeId){
            //I'm the leader, I return the value I've stored, even if null, and I specify if it's present in the answer message
            String value = this.data.get(message.key);
            boolean isPresent = value != null;
            message.replyTo
                    .tell(new GetAnswer(message.key, value, isPresent,ticket));
        }else if(this.replicas.containsKey(message.key)){
            //I'm not the leader but I do have a replica of the value, so that's good
            message.replyTo
                    .tell(new GetAnswer(message.key, replicas.get(message.key), true,ticket));
        }
        else {
            //I contact the leader and all the nodes which are supposed to keep a replica
            ActorRef<Command> leader = nodes.get(nodePosition).getNode();
            leader.tell(new Get(message.key, context.getSelf(), ticket));
            getSuccessorNodes(nodePosition,this.nReplicas, this.nodes)
                    .stream()
                    .map(NodeInfo::getNode)
                    .forEach(n -> n.tell(new Get(message.key, context.getSelf(), ticket)));
            requests.put(ticket, message.replyTo);

        }
        ticket++;
        return Behaviors.same();
    }

    private Behavior<Command> onGet(Get message){
        int nodePosition = message.key.hashCode() % nodes.size();
        if(nodePosition < 0 ) nodePosition += nodes.size();
        if (nodePosition == this.nodeId){
            //I'm the leader, I return the value I've stored, even if null, and I specify if it's present in the answer message
            String value = this.data.get(message.key);
            boolean isPresent = value != null;
            message.replyTo.tell(new GetAnswer(message.key, value, isPresent,message.requestId));
        }else if(this.replicas.containsKey(message.key)){
            //I'm not the leader but I do have a replica of the value, so that's good, I can answer
            message.replyTo.tell(new GetAnswer(message.key, replicas.get(message.key), true,message.requestId));
        }
        return Behaviors.same();
    }

    private Behavior<Command> onGetAnswer(GetAnswer message){
        if (requests.containsKey(message.requestId)){
            requests.remove(message.requestId)
                    .tell(new GetAnswer(message.key, message.value,message.isPresent, message.requestId));
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
        System.out.println(nodes.size() + " this is the size of the cluster");
        System.out.println(nodePosition + "   NODE POSITION for key " + message.key);
        nodes.sort(Comparator.comparing(NodeInfo::getHashKey));
        if (nodePosition == this.nodeId){
            //I'm the leader, so I add the value to my data
            this.data.put(message.key,message.value);
            message.replyTo.tell(new PutAnswer(true, ticket));
        }else{
            //I send the data to the leader of that data
            NodeInfo node = nodes.get(nodePosition);
            node.getNode().tell(new Put(message.key,message.value, context.getSelf(), false, ticket));
            requests.put(ticket, message.replyTo);
        }
        //add the replicas
        getSuccessorNodes(nodePosition,this.nReplicas,this.nodes).stream()
                .map(NodeInfo::getNode)
                .forEach(n -> n.tell(new Put(message.key,message.value, context.getSelf(), true, ticket)));
        ticket++;
        return Behaviors.same();
    }

    private Behavior<Command> onPutAnswer(PutAnswer message){
        if (requests.containsKey(message.requestId)){
            requests.remove(message.requestId)
                    .tell(new PutAnswer(message.success, message.requestId));
        }
        return Behaviors.same();
    }

    private Behavior<Command> onPut(Put message){
        if ( message.isReplica){
            this.replicas.put(message.key, message.value);
        }
        else{
            this.data.put(message.key,message.value);
            message.replyTo.tell(new PutAnswer(true, message.requestId));
        }
        return Behaviors.same();
    }


    private Behavior<Command> onNodesUpdate(NodesUpdate message) {
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

        //reassigning all the values
        HashMap<String,String> allData = new HashMap<>(this.data);
        allData.putAll(this.replicas);
        this.data.clear();
        this.replicas.clear();
        allData.entrySet()
            .stream()
            .forEach(entry -> {
                int nodePosition = entry.getKey().hashCode() % nodes.size();
                if(nodePosition < 0 ) nodePosition += nodes.size();
                nodes.sort(Comparator.comparing(NodeInfo::getHashKey));
                if (nodePosition == this.nodeId){
                    //I'm the leader, so I add the value to my data
                    this.data.put(entry.getKey(),entry.getValue());
                }else{
                    //I send the data to the leader of that data
                    ActorRef<Command> node = nodes.get(nodePosition).getNode();
                    node.tell(new Put(entry.getKey(), entry.getValue(), context.getSelf(), false, ticket));
                }
                //add the replicas
                getSuccessorNodes(nodePosition,this.nReplicas,this.nodes).stream()
                        .map(NodeInfo::getNode)
                        .forEach(n -> n.tell(new Put(entry.getKey(), entry.getValue(), context.getSelf(), true, ticket)));
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

    private List<NodeInfo> getSuccessorNodes(int nodeId, int nReplicas, List<NodeInfo> nodes){
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
        Collection<String> allData = new ArrayList<>(this.data.values());
        Collection<String> replicas = new ArrayList<>(this.replicas.values());
        allData.addAll(replicas);
        message.replyTo.tell(new GetAllLocalAnswer(allData));
        return Behaviors.same();
    }

    private Behavior<Command> onGetNodesRequest (GetNodesRequest message){
        message.replyTo.tell(new GetNodesAnswer(this.nodes));
        return Behaviors.same();
    }
}
