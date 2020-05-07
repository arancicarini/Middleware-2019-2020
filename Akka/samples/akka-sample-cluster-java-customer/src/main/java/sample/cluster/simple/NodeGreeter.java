package sample.cluster.simple;


import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.cluster.typed.Cluster;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

// #greeter
public class NodeGreeter extends AbstractBehavior<NodeGreeter.Command> {

    //messages
    public interface Command{}
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
        public Set<ActorRef<Command>> newNodes;

        public NodesUpdate (Set<ActorRef<Command>> newNodes){
            this.newNodes=newNodes;
        }
    }

    public static class Discover implements Command{
        public final ActorRef<Command> replyTo;

        public Discover(ActorRef<Command> replyTo){
            this.replyTo=replyTo;
        }
    }

    public static class Discovered implements Command{
        public final String address;
        public final String port;
        public final ActorRef<Command> ref;

        public Discovered(String address, String port, ActorRef<Command> ref){
            this.address=address;
            this.port=port;
            this.ref=ref;
        }
    }

    //actor attributes
    private final int max;
    private int greetingCounter;
    public static ServiceKey<Command> KEY= ServiceKey.create(Command.class, "NODE");
    private final HashMap<String, ActorRef<Command>> nodes;

    public static Behavior<Command> create(int max) {
        return Behaviors.setup(context -> {
            NodeGreeter nodeGreeter = new NodeGreeter(context, max);
            context.getSystem().receptionist().tell(Receptionist.register(KEY, context.getSelf().narrow()));
            ActorRef<Receptionist.Listing> subscriptionAdapter = context.messageAdapter(Receptionist.Listing.class, listing ->
                            new NodesUpdate(listing.getServiceInstances(NodeGreeter.KEY)));
            context.getSystem().receptionist().tell(Receptionist.subscribe(NodeGreeter.KEY,subscriptionAdapter));
            return nodeGreeter;
        });
    }

    private NodeGreeter(ActorContext<Command> context, int max) {
        super(context);
        this.max = max;
        this.greetingCounter = 0;
        this.nodes = new HashMap<>();
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder().
                onMessage(Greet.class, this::onGreet).
                onMessage(Greeted.class, this::onGreeted).
                onMessage(SayHello.class, this::onSayHello).
                onMessage(NodesUpdate.class, this:: onNodesUpdate).
                onMessage(Discover.class, this::onDiscover).
                onMessage(Discovered.class, this:: onDiscovered).
                build();
    }


    private Behavior<Command> onGreet(Greet message) {
        getContext().getLog().info("Fijne Dutch Liberation Day, {}!", message.whom);
        //#greeter-send-message
        message.replyTo.tell(new Greeted(message.whom,getContext().getSelf() ));
        //#greeter-send-message
        return this;
    }

    private Behavior<Command> onGreeted(Greeted message){
        greetingCounter++;
        getContext().getLog().info("Greetings {} have been delivered to  {} on Actor {}", greetingCounter, message.whom, message.replyTo);
        if (greetingCounter == max) {
            return Behaviors.stopped();
        } else {
            return this;
        }
    }


    //todo implement correctly this method
    private Behavior<Command> onSayHello(SayHello message) throws UnknownHostException {
        for (ActorRef<Command> node : nodes.values()){
            node.tell(new Greet(message.name, getContext().getSelf()));
        }

        return this;
    }


    private Behavior<Command> onNodesUpdate(NodesUpdate message) {
        Set<ActorRef<Command>> newNodesCopy= new HashSet<>(message.newNodes);
        message.newNodes.removeAll(nodes.values());
        for(ActorRef<Command> node: message.newNodes ){
            node.tell(new Discover(getContext().getSelf()));
        }

        Collection<ActorRef<Command>> toRemove = nodes.values();
        toRemove.removeAll(newNodesCopy);

        List<String> filtered= nodes.entrySet().stream().filter(e->toRemove.contains(e.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
        for(String key : filtered){
            nodes.remove(key);
        }
        getContext().getLog().info("List of services registered with the receptionist changed: {}", nodes);
        return this;
    }

    private Behavior<Command> onDiscover(Discover message){
        Cluster cluster = Cluster.get(getContext().getSystem());
        String IPaddress = cluster.selfMember().address().getHost().get();
        String port = String.valueOf(cluster.selfMember().address().getPort().get());
        message.replyTo.tell(new Discovered(IPaddress,port, getContext().getSelf()));
        return this;
    }

    private Behavior<Command> onDiscovered(Discovered message){
        String key = message.address + ":" + message.port;
        String hashKey = hashfunction(key);
        nodes.put(hashKey, message.ref);
        return this;
    }

    private static String hashfunction(String key){
        MessageDigest digest;
        byte[] hash;
        StringBuffer hexHash = new StringBuffer();
        try {
            // Create the SHA-1 of the nodeidentifier
            digest = MessageDigest.getInstance("SHA-1");
            hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            System.out.println(hash.length);
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