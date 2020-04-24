package sample.cluster.simple.extended;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;


public class NodeManager extends AbstractBehavior<NodeManager.SayHello> {

    public static class SayHello {
        public final String name;

        public SayHello(String name) {
            this.name = name;
        }
    }

    private final ActorRef<NodeGreeter.Command> greeter;

    public static Behavior<SayHello> create() {
        return Behaviors.setup(NodeManager::new);
    }

    private NodeManager(ActorContext<SayHello> context) {
        super(context);
        //#create-actors
        greeter = context.spawn(NodeGreeter.create(3), "greeter");
        //#create-actors
    }

    @Override
    public Receive<SayHello> createReceive() {
        return newReceiveBuilder().onMessage(SayHello.class, this::onSayHello).build();
    }

    private Behavior<SayHello> onSayHello(SayHello command) {
        //#create-actors
        ActorRef<Greeter.Command> replyTo =
                getContext().spawn(Greeter.create(3), command.name);
        greeter.tell(new NodeGreeter.Greet(command.name, replyTo));
        //#create-actors
        return this;
    }
}