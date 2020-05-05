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

public class App {
  private static ActorRef<NodeGreeter.Command> greeter;
  public static int port;
  public static void main(String[] args) {
    if (args.length == 0) {
      startup(25251);
      startup(25252);
      startup(0);
    } else
      Arrays.stream(args).map(Integer::parseInt).forEach(App::startup);

    //#main-send-messages
    //greeter.tell(new NodeGreeter.SayHello("Arianna"));
    //#main-send-messages
  }

  private static Behavior<Void> rootBehavior() {
    return Behaviors.setup(context -> {

      // Create an actor that handles cluster domain events
      context.spawn(ClusterListener.create(), "ClusterListener");
      ActorRef<NodeGreeter.Command> greeter = context.spawn(NodeGreeter.create(3), "NodeGreeter");
      UserRoutes userRoutes = new UserRoutes(context.getSystem(), greeter);
      startHttpServer(userRoutes.userRoutes(), context.getSystem());
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

    App.port = port;
    // Create an Akka system
    ActorSystem<Void> system = ActorSystem.create(rootBehavior(), "ClusterSystem", config);

  }

  private static void startHttpServer(Route route, ActorSystem<?> system) {
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
}
