package project;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.cluster.typed.Cluster;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import akka.actor.typed.javadsl.ActorContext;

import javax.xml.crypto.Data;

public class DataNode {

    public interface Command extends CborSerializable{}

    //messages

    public static final class GetRequest implements Command {
        String key;
        ActorRef<Command> replyTo;

        public GetRequest(String key, ActorRef<Command> replyTo){
            this.key=key;
            this.replyTo=replyTo;
        }
    }

    public static final class GetAnswer implements Command{
        String key;
        String value;
        //Boolean present;
        ActorRef<Command> replyTo;

        public GetAnswer(String key, String value, ActorRef<Command> replyTo){
            this.key = key;
            this.value = value;
            this.replyTo = replyTo;
        }
    }

    public static final class PutRequest implements Command{
        String key;
        String value;
        ActorRef<Command> replyTo;

        public PutRequest(List<String> parameters, ActorRef<Command> replyTo){
            this.key=parameters.get(0);
            this.value=parameters.get(1);
            this.replyTo=replyTo;
        }
    }

    public static final class PutAnswer implements Command{
        //Boolean done??
        ActorRef<Command> replyTo;

        public PutAnswer(ActorRef<Command> replyTo){
            this.replyTo=replyTo;
        }
    }

    public static class NodesUpdate implements Command{
        public Set<ActorRef<Command>> currentNodes;

        public NodesUpdate (Set<ActorRef<Command>> currentNodes){
            this.currentNodes=currentNodes;
        }
    }


    public static class Discover implements Command{
        public final ActorRef<Command> replyTo;

        @JsonCreator
        public Discover(@JsonProperty("replyTo")ActorRef<Command> replyTo){
            this.replyTo=replyTo;
        }
    }

    public static class Discovered implements Command{
        public final String address;
        public final String port;
        public final ActorRef<Command> replyTo;

        @JsonCreator
        public Discovered(@JsonProperty("address")String address,@JsonProperty("port") String port,@JsonProperty("replyTo")ActorRef<Command> replyTo){
            this.address=address;
            this.port=port;
            this.replyTo = replyTo;
        }
    }
    //-----------------------------------------------------------------------

    //actor attributes
    public final String port;
    public final String address;
    private final HashMap<String, ActorRef<Command>> nodes = new HashMap<>();
    public static ServiceKey<Command> KEY= ServiceKey.create(Command.class, "NODE");
    private final ActorContext<Command> context;


    public DataNode(String port, String address, ActorContext<Command> context) {
        this.port = port;
        this.address = address;
        this.context = context;
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(context -> {
            DataNode dataNode = new DataNode(context);
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
    private DataNode(@JsonProperty("context")ActorContext<Command> context) {
        this.context = context;
        Cluster cluster = Cluster.get(context.getSystem());
        this.address = cluster.selfMember().address().getHost().get();
        this.port = String.valueOf(cluster.selfMember().address().getPort().get());
    }

    //MessageHandler
    private Behavior<Command> behavior() {
        return Behaviors.receive(Command.class)
                .onMessage(GetRequest.class, this::onGetRequest).
                        onMessage(GetAnswer.class, this::onGetAnswer).
                        onMessage(PutRequest.class, this::onPutRequest).
                        onMessage(PutAnswer.class, this::onPutAnswer).
                        onMessage(NodesUpdate.class, this:: onNodesUpdate).
                        onMessage(Discover.class, this::onDiscover).
                        onMessage(Discovered.class, this:: onDiscovered).
                        build();
    }


    private Behavior<Command> onGetRequest(GetRequest message){
        return Behaviors.same();
    }

    private Behavior<Command> onGetAnswer(GetAnswer message){
        return Behaviors.same();
    }

    private Behavior<Command> onPutRequest(PutRequest message){
        return Behaviors.same();
    }

    private Behavior<Command> onPutAnswer(PutAnswer message){
        return Behaviors.same();
    }

    private Behavior<Command> onNodesUpdate(NodesUpdate message) {
        //send a discovery message to all new nodes added to the cluster--> maybe can be optimized
        Set<ActorRef<Command>> currentNodes= new HashSet<>(message.currentNodes);
        List<String> currentNodesIDs = currentNodes.stream().map(Object::toString).collect(Collectors.toList());
        Collection<ActorRef<Command>> oldNodes = nodes.values();
        List<String> oldNodesIDs = oldNodes.stream().map(Object::toString).collect(Collectors.toList());
        int i = 0;
        currentNodes.removeIf(n -> oldNodesIDs.contains(n.toString()));
        i = 0;
        for(ActorRef<Command> node: currentNodes){
            context.getLog().info("Nodes Update member {}: {}",i, node.toString());
            node.tell(new Discover(context.getSelf()));
            i++;
        }

        //removing all the nodes which are not reachable anymore
        oldNodes.removeIf(n -> currentNodesIDs.contains(n.toString()));
        List<String> keysToRemove= nodes.entrySet().stream().filter(e->oldNodes.contains(e.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
        for(String key : keysToRemove){
            nodes.remove(key);
        }

        //logging the new status of the cluster
        context.getLog().info("List of services registered with the receptionist changed due to potential delete, new list:");
        i=0;
        for (Map.Entry<String, ActorRef<Command>> node: nodes.entrySet()){
            context.getLog().info("Member  {}, member string,,value :  {},, {}",i, node.getKey(), node.getValue().toString());
            i++;
        }
        return Behaviors.same();
    }

    private Behavior<Command> onDiscover(Discover message){
        context.getLog().info("{} wants to discover me!", message.replyTo);
        message.replyTo.tell(new Discovered(address,port, context.getSelf()));
        return Behaviors.same();
    }

    private Behavior<Command> onDiscovered(Discovered message){
        String key = message.address + ":" + message.port;
        String hashKey = hashfunction(key);
        context.getLog().info("{} has been discovered!", message.replyTo);
        nodes.put(hashKey, message.replyTo);
        context.getLog().info("List of services registered with the receptionist changed due to discovered, new list:");
        int i=0;
        for (Map.Entry<String, ActorRef<Command>> node: nodes.entrySet()){
            context.getLog().info("Member  {}, member string,,value :  {},, {}",i, node.getKey(), node.getValue().toString());
            i++;
        }
        return Behaviors.same();
    }


    //----------------------------------------------------------------------------------
    //supporting function
    //converting ip port -> hash
    private static String hashfunction(String key){
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
