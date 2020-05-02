package sample.cluster.simple;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.cluster.Member;
import akka.cluster.typed.Cluster;
import akka.http.javadsl.Http;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.Materializer;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

// #greeter
public class NodeGreeter extends AbstractBehavior<NodeGreeter.Command> {
    public interface Command{}

    public static final class Greet implements Command{
        public final String whom;
        //public final ActorRef<Greeted> replyTo;

        public Greet(String whom) {
            this.whom = whom;
            //this.replyTo = replyTo;
        }

    }

    public static final class Greeted implements Command {
        public final String whom;

        public Greeted(String whom) {
            this.whom = whom;
        }

        // #greeter
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Greeted greeted = (Greeted) o;
            return Objects.equals(whom, greeted.whom);
        }

        @Override
        public int hashCode() {
            return Objects.hash(whom);
        }

        @Override
        public String toString() {
            return "Greeted{" +
                    "whom='" + whom +
                    '}';
        }
// #greeter
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

    private final int max;
    private int greetingCounter;

    public static Behavior<Command> create(int max) {
        return Behaviors.setup(context -> new NodeGreeter(context, max));
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
        getContext().getLog().info("Happy Liberation Day, {}!", message.whom);
        //#greeter-send-message
        //message.replyTo.tell(new Greeted(message.whom ));
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

    private Behavior<Command> onSayHello(SayHello sayHello){
        String name = sayHello.name;
        akka.actor.ActorSystem classicSystem = Adapter.toClassic(getContext().getSystem());
        final Materializer materializer = Materializer.matFromSystem(classicSystem);
        Cluster cluster = Cluster.get(getContext().getSystem());
        System.out.println(cluster.state().productPrefix());
        Iterable<Member> clusterMembers = cluster.state().getMembers();
        System.out.println("Members: "+ clusterMembers.toString());
        for(Member member: clusterMembers) {
            if (!member.equals(cluster.selfMember())){
                Optional<String> host = member.address().getHost();
                Optional<Integer> port = member.address().getPort();

                if (host.isPresent() && port.isPresent()) {
                    System.out.println("HOST: "+ host.get()+ "PORT:"+ port.get() );
                    final CompletionStage<HttpResponse> responseFuture =
                            Http.get(classicSystem)
                                    .singleRequest(HttpRequest.create("http://localhost:" + port.get()+ "/greet/" + name));
                    responseFuture.whenComplete((response, exception) -> {
                        CompletionStage<Greeted> greeted = Jackson.unmarshaller(Greeted.class).unmarshal(response.entity(), materializer);
                        greeted.whenComplete((message, exception1) -> {
                            getContext().getLog().info("Greetings {} have been delivered to  {}", greetingCounter, message.whom);
                        });
                    });
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