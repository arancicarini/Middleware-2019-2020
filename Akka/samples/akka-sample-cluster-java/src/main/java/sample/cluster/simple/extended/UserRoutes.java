package sample.cluster.simple.extended;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Scheduler;
import akka.actor.typed.javadsl.AskPattern;
import static akka.http.javadsl.server.Directives.*;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.PathMatchers;
import akka.http.javadsl.server.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Routes can be defined in separated classes like shown in here
 */
//#user-routes-class
public class UserRoutes {
    //#user-routes-class
    private final static Logger log = LoggerFactory.getLogger(UserRoutes.class);
    private final ActorRef<NodeGreeter.Command> greeter;
    private final Duration askTimeout;
    private final Scheduler scheduler;

    public UserRoutes(ActorSystem<?> system, ActorRef<NodeGreeter.Command> greeter) {
        this.greeter = greeter;
        scheduler = system.scheduler();
        askTimeout = system.settings().config().getDuration("my-app.routes.ask-timeout");
    }

    private CompletionStage<NodeGreeter.Greeted> greet(String name) {
        return AskPattern.ask(greeter, ref -> new NodeGreeter.Greet(name, ref), askTimeout, scheduler);
    }


    /**
     * This method creates one route (of possibly many more that will be part of your Web App)
     */
    //#all-routes
    public Route userRoutes() {
        return pathPrefix("greet", () ->
                concat(
                        //#greet- get
                        path(PathMatchers.segment(), (String name) ->
                                concat(
                                        get(() ->
                                                        //#answer with a greeted message
                                                        rejectEmptyResponse(() ->
                                                                onSuccess(greet(name), greeted ->
                                                                        complete(StatusCodes.OK, greeted, Jackson.marshaller())
                                                                )
                                                        )
                                                //#answer with a greeted message
                                        )
                                )
                        )
                        //#greet -get
                )
        );
    }
    //#all-routes

}

