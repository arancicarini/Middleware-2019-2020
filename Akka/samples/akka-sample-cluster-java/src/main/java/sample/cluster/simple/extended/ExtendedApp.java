package sample.cluster.simple.extended;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.w3c.dom.Node;
import sample.cluster.simple.ClusterListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ExtendedApp {
    private ActorRef<NodeManager.SayHello> nodeManager;
    public static void main(String[] args) {
        if (args.length == 0) {
            startup(25251);
            startup(25252);
            startup(0);
        } else
            Arrays.stream(args).map(Integer::parseInt).forEach(ExtendedApp::startup);
    }

    ActorRef<NodeManager.SayHello> nodeManager = context.sp


    //#main-send-messages
    greeterMain.tell(new GreeterMain.SayHello("Arianna"));
    //#main-send-messages

    try {
        System.out.println(">>> Press ENTER to exit <<<");
        System.in.read();
    } catch (
    IOException ignored) {
    } finally {
        greeterMain.terminate();
    }

    private static Behavior<Void> rootBehavior() {
        return Behaviors.setup(context -> {

            // Create an actor that handles cluster domain events
            context.spawn(ClusterListener.create(), "ClusterListener");
            nodeManager = context.spawn(NodeManager.create(), "NodeManager");
            return Behaviors.empty();
        });
    }

    private static void startup(int port) {
        // Override the configuration of the port
        // Override the configuration of the port
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("akka.remote.artery.canonical.port", port);

        Config config = ConfigFactory.parseMap(overrides)
                .withFallback(ConfigFactory.load());

        // Create an Akka system
        ActorSystem<Void> system = ActorSystem.create(rootBehavior(), "ClusterSystem", config);
    }
}