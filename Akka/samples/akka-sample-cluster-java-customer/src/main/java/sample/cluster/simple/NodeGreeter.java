package sample.cluster.simple;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.cluster.Member;
import akka.cluster.typed.Cluster;
import akka.remote.artery.compress.InboundActorRefCompression;
import akka.stream.Materializer;
import org.w3c.dom.Node;

import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

// #greeter
public class NodeGreeter extends AbstractBehavior<NodeGreeter.Command> {

    //messages
    public interface Command{}
    public static final class Greet implements Command{
        public final String whom;
        public final ActorRef<Greeted> replyTo;

        public Greet(String whom, ActorRef<Greeted> replyTo) {
            this.whom = whom;
            this.replyTo = replyTo;
        }

    }

    public static final class Greeted implements Command {
        public final String whom;
        public Greeted(String whom) {
            this.whom = whom;
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
    private HashMap<String, ActorRef<Command>> nodes;

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
        message.replyTo.tell(new Greeted(message.whom ));
        //#greeter-send-message
        return this;
    }

    private Behavior<Command> onGreeted(Greeted message){
        greetingCounter++;
        getContext().getLog().info("Greetings {} have been delivered to  {}", greetingCounter, message.whom);
        if (greetingCounter == max) {
            return Behaviors.stopped();
        } else {
            return this;
        }
    }


    private Behavior<Command> onSayHello(SayHello sayHello) throws UnknownHostException {
        String name = sayHello.name;
        akka.actor.ActorSystem classicSystem = Adapter.toClassic(getContext().getSystem());
        final Materializer materializer = Materializer.matFromSystem(classicSystem);
        Cluster cluster = Cluster.get(getContext().getSystem());
        Iterable<Member> clusterMembers = cluster.state().getMembers();
        getContext().getLog().info("Members: "+ clusterMembers.toString());
        for(Member member: clusterMembers) {
            if (!member.equals(cluster.selfMember())){
                Optional<String> host = member.address().getHost();
                Optional<Integer> port = member.address().getPort();
                if (host.isPresent() && port.isPresent()) {

                    ServiceKey<Command> serviceKey= ServiceKey.create(Command.class,host.get()+":"+ port.get());
                    getContext().getSystem().receptionist().tell(Receptionist.find(serviceKey,getContext().getSelf());
                    getContext().getLog().info("HOST: "+ host.get()+ " PORT:"+ port.get());
                } else {
                    getContext().getLog().info("Not possibile to send a message");
                }
            }
        }
        sayHello.replyTo.tell(new SaidHello(name));

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
        nodes.values().removeAll(toRemove);

        List<String> filtered= nodes.entrySet().stream().filter(e->toRemove.contains(e.getValue())).map(e->e.getKey()).collect(Collectors.toList());
        for(String key : filtered){
            nodes.remove(key);
        }
        return this;
    }

    private Behavior<Command> onDiscover(Discover message){
        return this;
    }

    private Behavior<Command> onDiscovered(Discovered message){
        return this;
    }




}
// #greeter