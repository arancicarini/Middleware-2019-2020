package project;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Scheduler;
import akka.actor.typed.javadsl.AskPattern;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
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
    private final ActorRef<DataNode.Command> node;
    private final Duration askTimeout;
    private final Scheduler scheduler;

    public UserRoutes(ActorSystem<?> system, ActorRef<DataNode.Command> node) {
        this.node = node;
        scheduler = system.scheduler();
        askTimeout = system.settings().config().getDuration("akka.routes.ask-timeout");
    }

    private CompletionStage<DataNode.Command> getRequest(String key) {
        return AskPattern.ask(node, ref -> new DataNode.GetRequest(key, ref), askTimeout, scheduler);
    }

    private CompletionStage<DataNode.Command> putRequest(String key, String value) {
        return AskPattern.ask(node, ref -> new DataNode.PutRequest(key, value, ref), askTimeout, scheduler);
    }


    /**
     * This method creates one route (of possibly many more that will be part of your Web App)
     */
    //#all-routes
    public Route userRoutes() {
        return concat(
             //#greet/$id - get
            pathPrefix("get",  () ->
                parameter("key", (String key) ->
                    get(() ->
                        //#answer with a getAnswer message marshalled with Jackson
                        onSuccess(getRequest(key), getAnswer -> {
                            log.info("Get API Endpoint called");
                            return complete(StatusCodes.OK, getAnswer, Jackson.marshaller());
                        })
                    )
                )
            ),
            //#sayHello/$id - get
            pathPrefix("put",() ->
                parameter("key", (String key) ->
                    parameter("value", (String value)->
                        get(() ->
                            //#answer with a putAnswer message marshalled with Jackson
                            onSuccess(putRequest(key,value), putAnswer->{
                                log.info("Put API Endpoint called");
                                return complete(StatusCodes.OK, putAnswer, Jackson.marshaller());
                            })
                        )
                    )
                )
            )
        );
    }
    //#all-routes
}

