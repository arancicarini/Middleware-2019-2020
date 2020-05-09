package sample.cluster.simple;


import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorRefResolver;
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
        context.getLog().info("Fijne Dutch Liberation Day, {}!", message.whom);
        //#greeter-send-message
        message.replyTo.tell(new Greeted(message.whom,context.getSelf() ));
        //#greeter-send-message
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
        Collection<ActorRef<Command>> oldNodes = nodes.values();
        currentNodes.removeAll(oldNodes);
        int i = 0;
        for(ActorRef<Command> node: message.currentNodes){
            context.getLog().info("Nodes Update member {}: {}",i, node);
            node.tell(new Discover(context.getSelf()));
            i++;
        }

        //removing all the nodes which are not reachable anymore
        currentNodes= new HashSet<>(message.currentNodes);
        oldNodes.removeAll(currentNodes);
        List<String> keysToRemove= nodes.entrySet().stream().filter(e->oldNodes.contains(e.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
        for(String key : keysToRemove){
            nodes.remove(key);
        }
        //logging the new status of the cluster
        context.getLog().info("List of services registered with the receptionist changed, new list:");
        i=0;
        for (ActorRef<Command> node: nodes.values()){
            context.getLog().info("Member {}: {}",i, nodes);
            i++;
        }


        return Behaviors.same();
    }

    private Behavior<Command> onDiscover(Discover message){
        Cluster cluster = Cluster.get(context.getSystem());
        String IPaddress = cluster.selfMember().address().getHost().get();
        String port = String.valueOf(cluster.selfMember().address().getPort().get());
        context.getLog().info("{} wants to discover me!", message.replyTo);
        message.replyTo.tell(new Discovered(IPaddress,port, context.getSelf()));
        return Behaviors.same();
    }

    private Behavior<Command> onDiscovered(Discovered message){
        String key = message.address + ":" + message.port;
        String hashKey = hashfunction(key);
        context.getLog().info("{} has been discovered!", message.replyTo);
        nodes.put(hashKey, message.replyTo);
        context.getLog().info("List of services registered with the receptionist changed, new list:");
        int i=0;
        for (ActorRef<Command> node: nodes.values()){
            context.getLog().info("Member {}: {}",i, nodes);
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




}
// #greeter