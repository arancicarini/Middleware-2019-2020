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
        askTimeout = system.settings().config().getDuration("akka.routes.ask-timeout");
    }

    private CompletionStage<NodeGreeter.Greeted> greet(String name) {
        return AskPattern.ask(greeter, ref -> new NodeGreeter.Greet(name), askTimeout, scheduler);
    }

    private CompletionStage<NodeGreeter.SaidHello> sayHello(String name) {
        return AskPattern.ask(greeter, ref -> new NodeGreeter.SayHello(name, ref), askTimeout, scheduler);
    }



    /**
     * This method creates one route (of possibly many more that will be part of your Web App)
     */
    //#all-routes
    public Route userRoutes() {
        return concat(pathPrefix("greet",  () ->
                        //#greet- get
                        path(PathMatchers.segment(), (String name) ->
                                        get(() ->
                                                        //#answer with a greeted message
                                                {greeter.tell(new NodeGreeter.Greet(name) );
                                                    return complete(StatusCodes.OK);
                                                                        }
                                                                )

                                                //#answer with a greeted message
                                        )

                        )
                        //#greet -get
                        , pathPrefix("sayHello",() ->
                        //#greet- get
                        path(PathMatchers.segment(), (String name) ->
                                get(() ->
                                                //#answer with a greeted message
                                                onSuccess(sayHello(name), saidHello ->{
                                                            System.out.println("API ENDPOINT CALLED");
                                                            return complete(StatusCodes.OK, saidHello, Jackson.marshaller());

                                                        }
                                                )

                                        //#answer with a greeted message
                                )

                        )
                //#greet -get
        ));
    }
    //#all-routes

}

