package sample.cluster.simple;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.io.Tcp;
import akka.io.TcpMessage;
import java.net.InetSocketAddress;

public class TCPServer extends AbstractActor {
    final akka.actor.typed.ActorRef<NodeGreeter.Command> nodeGreeter;
    final ActorRef manager;

    public TCPServer(ActorRef manager, akka.actor.typed.ActorRef<NodeGreeter.Command> nodeGreeter) {
        this.manager = manager;
        this.nodeGreeter = nodeGreeter;
    }

    public static Props props(ActorRef manager,akka.actor.typed.ActorRef<NodeGreeter.Command> nodeGreeter) {
        return Props.create(TCPServer.class, manager, nodeGreeter);
    }

    @Override
    public void preStart() throws Exception {
        final ActorRef tcp = Tcp.get(getContext().getSystem()).manager();
        tcp.tell(TcpMessage.bind(getSelf(), new InetSocketAddress("localhost", 0), 100), getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        Tcp.Bound.class,
                        msg -> {
                            manager.tell(msg, getSelf());
                        })
                .match(
                        Tcp.CommandFailed.class,
                        msg -> {
                            getContext().stop(getSelf());
                        })
                .match(
                        Tcp.Connected.class,
                        conn -> {
                            manager.tell(conn, getSelf());
                            getSender().tell(TcpMessage.register(nodeGreeter), getSelf());
                        })
                .build();
    }
}
