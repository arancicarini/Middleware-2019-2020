package sample.cluster.simple;


import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.cluster.typed.Cluster;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import sample.cluster.CborSerializable;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;


// #greeter
public class NodeGreeter  {

    //messages
    public interface Command extends CborSerializable{};
    public static final class Greet implements Command{
        public final String whom;
        public final ActorRef<Command> replyTo;

        public Greet(String whom, ActorRef<Command> replyTo) {
            this.whom = whom;
            this.replyTo = replyTo;
        }

    }

    public static final class Greeted implements Command {
        public final String whom;
        public final ActorRef<Command> replyTo;
        public Greeted(String whom, ActorRef<Command> replyTo) {
            this.whom = whom;
            this.replyTo = replyTo;
        }
    }

    public static class SayHello implements Command {
        public final String name;
        public final ActorRef<SaidHello> replyTo;

        public SayHello(String name, ActorRef<SaidHello> replyTo) {
            this.name = name;
            this.replyTo = replyTo;
        }
    }

    public static class SaidHello implements Command{
        public final String name;

        public SaidHello(String name) {
            this.name = name;
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
    private final int max;
    private int greetingCounter;
    private final ActorContext<Command> context;
    public static ServiceKey<Command> KEY= ServiceKey.create(Command.class, "NODE");
    public final String port;
    public final String address;
    private final HashMap<String, ActorRef<Command>> nodes = new HashMap<>();

    public static Behavior<Command> create(int max) {
        return Behaviors.setup(context -> {
            NodeGreeter nodeGreeter = new NodeGreeter(context, max);
            context.getSystem().receptionist().tell(Receptionist.register(KEY, context.getSelf()));
            context.getLog().info("registering with the receptionist...");
            ActorRef<Receptionist.Listing> subscriptionAdapter = context.messageAdapter(Receptionist.Listing.class, listing ->
                            new NodesUpdate(listing.getServiceInstances(NodeGreeter.KEY)));
            context.getLog().info("subscribing with the receptionist...");
            context.getSystem().receptionist().tell(Receptionist.subscribe(NodeGreeter.KEY,subscriptionAdapter));
            return nodeGreeter.behavior();
        });
    }

    @JsonCreator
    private NodeGreeter(@JsonProperty("context")ActorContext<Command> context, @JsonProperty("max") int max) {
        this.context = context;
        this.max = max;
        this.greetingCounter = 0;
        Cluster cluster = Cluster.get(context.getSystem());
        this.address = cluster.selfMember().address().getHost().get();
        this.port = String.valueOf(cluster.selfMember().address().getPort().get());
    }

    private Behavior<Command> behavior() {
        return Behaviors.receive(Command.class)
            .onMessage(Greet.class, this::onGreet).
            onMessage(Greeted.class, this::onGreeted).
            onMessage(SayHello.class, this::onSayHello).
            onMessage(NodesUpdate.class, this:: onNodesUpdate).
            onMessage(Discover.class, this::onDiscover).
            onMessage(Discovered.class, this:: onDiscovered).
            build();
    }





    private Behavior<Command> onGreet(Greet message) {
        context.getLog().info("Liberation Day, {}!", message.whom);
        message.replyTo.tell(new Greeted(message.whom,context.getSelf() ));
        return Behaviors.same();
    }

    private Behavior<Command> onGreeted(Greeted message){
        greetingCounter++;
        context.getLog().info("Greetings {} have been delivered to  {} on Actor {}", greetingCounter, message.whom, message.replyTo);
        if (greetingCounter == max) {
            return Behaviors.stopped();
        } else {
            return Behaviors.same();
        }
    }



    private Behavior<Command> onSayHello(SayHello message) throws UnknownHostException {
        for (ActorRef<Command> node : nodes.values()){
            node.tell(new Greet(message.name, context.getSelf()));
        }
        message.replyTo.tell(new SaidHello(message.name));
        return Behaviors.same();
    }


    private Behavior<Command> onNodesUpdate(NodesUpdate message) {
        //send a discovery message to all new nodes added to the cluster
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
        if (o instanceof NodeGreeter){
            NodeGreeter toCompare = (NodeGreeter) o;
            return  this.toString().equals(toCompare.toString());
        }
        return false;
    }

    @Override
    public int hashCode(){
        return Objects.hash(this.toString());
    }





}
// #greeter