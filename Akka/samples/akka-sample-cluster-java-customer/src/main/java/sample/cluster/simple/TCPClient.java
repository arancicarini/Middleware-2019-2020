package sample.cluster.simple;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.actor.typed.ActorRef;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.util.ByteString;

import java.net.InetSocketAddress;

public class TCPClient extends AbstractActor {

    final InetSocketAddress remote;
    final ActorRef<NodeGreeter.Command> nodeGreeter;

    public static Props props(InetSocketAddress remote, ActorRef nodeGreeter) {
        return Props.create(TCPClient.class, remote, nodeGreeter);
    }

    public TCPClient(InetSocketAddress remote, ActorRef nodeGreeter) {
        this.remote = remote;
        this.nodeGreeter = nodeGreeter;

        final akka.actor.ActorRef tcp = Tcp.get(getContext().getSystem()).manager();
        tcp.tell(TcpMessage.connect(remote), getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        Tcp.CommandFailed.class,
                        msg -> {
                            nodeGreeter.tell(new NodeGreeter.FailedConnection(msg.toString(), getSelf()));
                            getContext().stop(getSelf());
                        })
                .match(
                        Tcp.Connected.class,
                        msg -> {
                            nodeGreeter.tell(new NodeGreeter.SuccessfulConnection(msg, getSelf()));
                            getSender().tell(TcpMessage.register(getSelf()), getSelf());
                            getContext().become(connected( getSender()));
                        })
                .build();
    }

    private Receive connected(final akka.actor.ActorRef connection) {
        return receiveBuilder()
                .match(
                        ByteString.class,
                        msg -> {

                            connection.tell(TcpMessage.write((ByteString) msg), getSender());
                        })
                .match(
                        Tcp.CommandFailed.class,
                        msg -> {
                            // OS kernel socket buffer was full
                        })
                .match(
                        Tcp.Received.class,
                        msg -> {
                            nodeGreeter.tell(new NodeGreeter.Received(msg.data()));
                        })
                .matchEquals(
                        "close",
                        msg -> {
                            connection.tell(TcpMessage.close(), getSelf());
                        })
                .match(
                        Tcp.ConnectionClosed.class,
                        msg -> {
                            getContext().stop(getSelf());
                        })
                .build();
    }
}
