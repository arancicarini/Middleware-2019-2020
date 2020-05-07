package sample.cluster.simple;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Scheduler;
import akka.actor.typed.javadsl.AskPattern;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.PathMatchers;
import akka.http.javadsl.server.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

import static akka.http.javadsl.server.Directives.*;

/**
 * This is a separate class used to define routes, the API endpoints
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
        askTimeout = system.settings().config().getDuration("akka.routes.ask-timeout");
    }

    private CompletionStage<NodeGreeter.Command> greet(String name) {
        return AskPattern.ask(greeter, ref -> new NodeGreeter.Greet(name, ref), askTimeout, scheduler);
    }

    private CompletionStage<NodeGreeter.SaidHello> sayHello(String name) {
        return AskPattern.ask(greeter, ref -> new NodeGreeter.SayHello(name, ref), askTimeout, scheduler);
    }



    /**
     * This method creates one route (of possibly many more that will be part of your Web App)
     */
    //#all-routes
    public Route userRoutes() {
        return concat(
            //#greet/$id - get
            pathPrefix("greet",  () ->
                path(PathMatchers.segment(), (String name) ->
                    get(() ->
                        //#answer with a greeted message marshalled with Jackson
                        onSuccess(greet(name), greeted -> {
                            log.info("Greet API REndpoint called");
                            return complete(StatusCodes.OK, greeted, Jackson.marshaller());
                            }
                        )
                    )
                )
            ),
            //#sayHello/$id - get
            pathPrefix("sayHello",() ->
                path(PathMatchers.segment(), (String name) ->
                    get(() ->
                        //#answer with a saidHello message marshalled with Jackson
                        onSuccess(sayHello(name), saidHello ->{
                            log.info("API ENDPOINT CALLED");
                            return complete(StatusCodes.OK, saidHello, Jackson.marshaller());
                             }
                        )
                    )
                )
            )
        );
    }
    //#all-routes

}

