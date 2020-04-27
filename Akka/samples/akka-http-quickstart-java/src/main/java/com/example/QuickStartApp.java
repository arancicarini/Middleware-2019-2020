package sample.cluster.simple;

import akka.NotUsed;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Adapter;
import akka.actor.typed.javadsl.Behaviors;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.Route;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;


public class QuickStartApp {
    private static ActorRef<NodeGreeter.Command> greeter;
    private static ActorRef<ClusterListener.Event> listener;
    private static ActorSystem<NotUsed> system;
    public static int port;
    static void startHttpServer(Route route, ActorSystem<?> system) {
        // Akka HTTP still needs a classic ActorSystem to start
        akka.actor.ActorSystem classicSystem = Adapter.toClassic(system);
        final Http http = Http.get(classicSystem);
        final Materializer materializer = Materializer.matFromSystem(system);

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = route.flow(classicSystem, materializer);
        CompletionStage<ServerBinding> futureBinding =
                http.bindAndHandle(routeFlow, ConnectHttp.toHost("localhost", port), materializer);

        futureBinding.whenComplete((binding, exception) -> {
            if (binding != null) {
                InetSocketAddress address = binding.localAddress();
                system.log().info("Server online at http://{}:{}/",
                        address.getHostString(),
                        address.getPort());
            } else {
                system.log().error("Failed to bind HTTP endpoint, terminating system", exception);
                system.terminate();
            }
        });
    }
    public static void main(String[] args) {
        if (args.length == 0) {
            startup(0);
        } else {
            Arrays.stream(args).map(Integer::parseInt).forEach(QuickStartApp::startup);
        }


        //#main-send-messages
        //#main-send-messages



    }

    private static Behavior<NotUsed> rootBehavior() {
        return Behaviors.setup(context -> {

            // Create an actor that handles cluster domain events
            greeter = context.spawn(NodeGreeter.create(3), "greeter");
            listener = context.spawn(ClusterListener.create(), "listener");
            UserRoutes userRoutes = new UserRoutes(context.getSystem(), greeter);
            startHttpServer(userRoutes.userRoutes(), context.getSystem());
            greeter.tell(new NodeGreeter.SayHello("Arianna"));
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

        QuickStartApp.port = port;
        // Create an Akka system
        system = ActorSystem.create(rootBehavior(), "HelloAkkaHttpServer", config);
        system.log().info("putting the server online at http://localhost:{}/",
                port);
    }
}