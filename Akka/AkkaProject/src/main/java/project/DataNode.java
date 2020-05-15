package project;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.cluster.typed.Cluster;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DataNode {
    private static final String IPADDRESS = "(([0-9]{3})(\\.)([0-9]{3})(\\.)([0-9]{1,3})(\\.)([0-9]{1,3}))";
    private static final String PORT = "([0-9]{5})";
    private static final String DATANODENUMBER = "([0-9]+)";
    private static final String NODEPATTERN = "Actor[akka://ClusterSystem@" + IPADDRESS + ":" + PORT + "/user/DataNode#-" + DATANODENUMBER +"]";
    private static final String IDENTIFIER = IPADDRESS + ":" + PORT;


    public interface Command extends CborSerializable{}

    //messages

    public static final class GetRequest implements Command {
        final String key;
        final ActorRef<Command> replyTo;

        public GetRequest(String key, ActorRef<Command> replyTo){
            this.key=key;
            this.replyTo=replyTo;
        }
    }

    public static final class GetAnswer implements Command{
        final String key;
        final String value;
        final boolean isPresent;
        final ActorRef<Command> replyTo;
        final int requestID;


        public GetAnswer(String key, String value,boolean isPresent, ActorRef<Command> replyTo, int requestID){
            this.key = key;
            this.value = value;
            this.isPresent = isPresent;
            this.replyTo = replyTo;
            this.requestID = requestID;
        }
    }

    public static final class Get implements Command{
        final String key;
        final ActorRef<Command> replyTo;
        final int requestID;

        public Get(String key, ActorRef<Command> replyTo, int requestID){
            this.key=key;
            this.replyTo=replyTo;
            this.requestID = requestID;
        }
    }

    public static final class PutRequest implements Command{
        final String key;
        final String value;
        final ActorRef<Command> replyTo;

        public PutRequest(String key, String value, ActorRef<Command> replyTo){
            this.key=key;
            this.value=value;
            this.replyTo=replyTo;
        }
    }

    public static final class PutAnswer implements Command{
        final ActorRef<Command> replyTo;
        final String Success;

        @JsonCreator
        public PutAnswer(@JsonProperty ("replyTo") ActorRef<Command> replyTo){
            this.replyTo=replyTo;
            this.Success = "hurray!!!";
        }
    }

    public static final class Put implements Command{
        final String key;
        final String value;
        final ActorRef<Command> replyTo;
        final boolean isReplica;

        @JsonCreator
        public Put(String key, String value, ActorRef<Command> replyTo, boolean isReplica) {
            this.key = key;
            this.value = value;
            this.replyTo = replyTo;
            this.isReplica = isReplica;
        }
    }

    public static class NodesUpdate implements Command{
        final public Set<ActorRef<Command>> currentNodes;

        public NodesUpdate (Set<ActorRef<Command>> currentNodes){
            this.currentNodes=currentNodes;
        }
    }

    //-----------------------------------------------------------------------

    //actor attributes
    private final String port;
    private final String address;
    private final int nReplicas;
    private int nNodes;
    private int nodeID;
    private final List<NodeInfo> nodes = new ArrayList<>();
    public static ServiceKey<Command> KEY= ServiceKey.create(Command.class, "NODE");
    private final ActorContext<Command> context;
    private final HashMap<String,String> data = new HashMap<>();
    private final HashMap<String,String> replicas = new HashMap<>();
    private final HashMap<Integer, ActorRef<Command>> getRequests = new HashMap<>();


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

    @JsonCreator
    private DataNode(@JsonProperty("context")ActorContext<Command> context, @JsonProperty("nReplicas") int nReplicas) {
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
        this.nodeID = 0;
        this.nNodes = 1;

    }

    //MessageHandler
    private Behavior<Command> behavior() {
        return Behaviors.receive(Command.class)
                .onMessage(GetRequest.class, this::onGetRequest).
                        onMessage(GetAnswer.class, this::onGetAnswer).
                        onMessage(PutRequest.class, this::onPutRequest).
                        onMessage(PutAnswer.class, this::onPutAnswer).
                        onMessage(NodesUpdate.class, this:: onNodesUpdate).
                        onMessage(Put.class, this::onPut).
                        build();
    }


    private Behavior<Command> onGetRequest(GetRequest message){
        String key = message.key;
        int parsedKey = key.hashCode();
        int nodePosition = parsedKey % nNodes;
        nodes.sort(Comparator.comparing(NodeInfo::getHashKey));
        if (nodePosition == this.nodeID){
            String value = this.data.get(message.key);
            if ( value == null){
                message.replyTo.tell(new GetAnswer(message.key, null, false, context.getSelf(),-1));
            }
            else{
                message.replyTo.tell(new GetAnswer(message.key, value, true, context.getSelf(),-1));
            }
        }else if(this.replicas.containsKey(message.key)) message.replyTo.tell(new GetAnswer(message.key, replicas.get(message.key), true, context.getSelf(),-1));
        else {
            List<ActorRef<Command>> selectedNodes = getSuccessorNodes(nodePosition,this.nReplicas, this.nodes).stream().map(NodeInfo::getNode).collect(Collectors.toList());;
            selectedNodes.stream().forEach(n ->{
                int requestID = getRequests.keySet().size();
                getRequests.put(requestID, n);
                n.tell(new Get(message.key, context.getSelf(), requestID));
            });
        }



        return Behaviors.same();
    }

    private Behavior<Command> onGetAnswer(GetAnswer message){
        if (getRequests.containsKey(message.requestID)){
            getRequests.remove(message.requestID).tell(new GetAnswer(message.key, message.value,true, context.getSelf(),-1));
        }
        return Behaviors.same();
    }


    /*---------------------------------------------------------------
    PUT
     */

    private Behavior<Command> onPutRequest(PutRequest message){
        String key = message.key;
        int parsedKey = key.hashCode();
        int nodePosition = parsedKey % nNodes;
        nodes.sort(Comparator.comparing(NodeInfo::getHashKey));
        if (nodePosition == this.nodeID){
            this.data.put(message.key,message.value);
        }else{
            NodeInfo node = nodes.get(nodePosition);
            node.getNode().tell(new Put(message.key,message.value, context.getSelf(), false));
        }
        message.replyTo.tell(new PutAnswer(context.getSelf()));
        return Behaviors.same();
    }

    private Behavior<Command> onPutAnswer(PutAnswer message){
        return Behaviors.same();
    }

    private Behavior<Command> onPut(Put message){
        if ( message.isReplica){
            this.replicas.put(message.key, message.value);
        }
        else{
            this.data.put(message.key,message.value);
            List<ActorRef<Command>> selectedNodes = getSuccessorNodes(this.nodeID,this.nReplicas,this.nodes).stream().map(NodeInfo::getNode).collect(Collectors.toList());
            selectedNodes.stream().forEach(n -> n.tell(new Put(message.key,message.value, context.getSelf(), true)));
        }
        return Behaviors.same();
    }


    private Behavior<Command> onNodesUpdate(NodesUpdate message) {
        this.nodes.clear();
        for (ActorRef<Command> node: message.currentNodes){
            Pattern pattern = Pattern.compile(IDENTIFIER);
            Matcher matcher = pattern.matcher(node.toString());
            if (matcher.find()){
                String identifier = matcher.group(0);
                System.out.println(identifier);
                String[] splittedIdentifier = identifier.split(":");
                System.out.println(splittedIdentifier[0]);
                System.out.println(splittedIdentifier[1]);
                String hashKey = hashfunction(splittedIdentifier[0], splittedIdentifier[1]);
                this.nodes.add(new NodeInfo(hashKey, node));
            }
        }

        this.nNodes = message.currentNodes.size();
        nodes.sort(Comparator.comparing(NodeInfo::getHashKey));
        int i =0;
        for (NodeInfo node: nodes){
            if (node.getNode().toString() == context.getSelf().toString()){
                nodeID = i;
            }
            i++;
        }
        HashMap<String,String> data = new HashMap<>(this.data);
        data.putAll(this.replicas);
        this.data.clear();
        this.replicas.clear();
        data.entrySet().stream().forEach(n ->{
            String key = n.getKey();
            int parsedKey = key.hashCode();
            int nodePosition = parsedKey % nNodes;
            if (nodePosition == this.nodeID){
                this.data.put(n.getKey(),n.getValue());
            }else{
                NodeInfo node = nodes.get(nodePosition);
                node.getNode().tell(new Put(n.getKey(),n.getValue(), context.getSelf(), false));
            }
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
            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff &  hash[i]);
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
        while(i <= nReplicas && i < nNodes){
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
}
