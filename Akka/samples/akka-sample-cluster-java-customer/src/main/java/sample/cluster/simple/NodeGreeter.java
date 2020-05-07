package sample.cluster.simple;

import akka.actor.Actor;
import akka.actor.ActorSelection;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.receptionist.Receptionist;
import akka.cluster.Member;
import akka.cluster.typed.Cluster;
import akka.http.javadsl.Http;
import akka.http.javadsl.OutgoingConnection;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.util.ByteString;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import static akka.http.javadsl.ConnectHttp.toHost;

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

    //actor attributes
    private final int max;
    private int greetingCounter;

    public static Behavior<Command> create(int max) {
        return Behaviors.setup(context -> {
            NodeGreeter nodeGreeter = new NodeGreeter(context, max);
            context.getSystem().receptionist().tell(Receptionist.register());
            return new NodeGreeter(context, max);
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
                    getContext().getLog().info("HOST: "+ host.get()+ " PORT:"+ port.get());
                } else {
                    getContext().getLog().info("Not possibile to send a message");
                }
            }
        }
        sayHello.replyTo.tell(new SaidHello(name));

        return this;
    }


}
// #greeter