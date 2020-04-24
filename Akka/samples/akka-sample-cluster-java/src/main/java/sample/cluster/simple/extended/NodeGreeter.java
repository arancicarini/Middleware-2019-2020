package sample.cluster.simple.extended;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Objects;

// #greeter
public class NodeGreeter extends AbstractBehavior<NodeGreeter.Command> {
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
        public final ActorRef<Command> from;

        public Greeted(String whom, ActorRef<Command> from) {
            this.whom = whom;
            this.from = from;
        }

        // #greeter
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Greeted greeted = (Greeted) o;
            return Objects.equals(whom, greeted.whom) &&
                    Objects.equals(from, greeted.from);
        }

        @Override
        public int hashCode() {
            return Objects.hash(whom, from);
        }

        @Override
        public String toString() {
            return "Greeted{" +
                    "whom='" + whom + '\'' +
                    ", from=" + from +
                    '}';
        }
// #greeter
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
                build();
    }

    private Behavior<Command> onGreet(Greet message) {
        getContext().getLog().info("Happy Liberation Day, {}!", message.whom);
        //#greeter-send-message
        message.replyTo.tell(new Greeted(message.whom, getContext().getSelf()));
        //#greeter-send-message
        return this;
    }

    private Behavior<Command> onGreeted(Greeted message){
        greetingCounter++;
        getContext().getLog().info("Greetings {} have been delivered to  {}", greetingCounter, message.whom);
        if (greetingCounter == max) {
            return Behaviors.stopped();
        } else {
            message.from.tell(new NodeGreeter.Greet(message.whom, getContext().getSelf()));
            return this;
        }
    }


}
// #greeter