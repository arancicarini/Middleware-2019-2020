package sample.cluster.simple.extended;

import akka.actor.ActorSystem;
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
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

// #greeter
public class NodeGreeter extends AbstractBehavior<NodeGreeter.Command> {
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
        @JsonCreator
        public Greeted(@JsonProperty("whom") String whom) {
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
        public SayHello(String name) {
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

    private Behavior<Command> onSayHello(SayHello sayHello){
        String name = sayHello.name;
        akka.actor.ActorSystem classicSystem = Adapter.toClassic(getContext().getSystem());
        final Materializer materializer = Materializer.matFromSystem(classicSystem);
        Cluster cluster = Cluster.get(getContext().getSystem());
        Iterable<Member> clusterMembers = cluster.state().getMembers();
        for(Member member: clusterMembers){
            Optional<String> host = member.address().getHost();
            Optional<Integer> port = member.address().getPort();
            if ( host.isPresent() && port.isPresent()){
                final CompletionStage<HttpResponse> responseFuture =
                        Http.get(classicSystem)
                                .singleRequest(HttpRequest.create(String.valueOf(host) + "/" + name + ":" + String.valueOf(port)));
                responseFuture.whenComplete((response, exception)-> {
                    CompletionStage<Greeted> greeted = Jackson.unmarshaller(Greeted.class).unmarshal(response.entity(), materializer);
                    greeted.whenComplete((message, exception1)->{
                        getContext().getLog().info("Greetings {} have been delivered to  {}", greetingCounter, message.whom);
                    });
                });
            }else {
                getContext().getLog().info("Not possibile to send a message");
            }
        }
        return this;
    }


}
// #greeter