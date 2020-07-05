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
//#test-routes-class
public class TestRoutes {
    //#user-routes-class
    private final static Logger log = LoggerFactory.getLogger(UserRoutes.class);
    private final ActorRef<DataNode.Command> node;
    private final Duration askTimeout;
    private final Scheduler scheduler;

    public TestRoutes(ActorSystem<?> system, ActorRef<DataNode.Command> node) {
        this.node = node;
        scheduler = system.scheduler();
        askTimeout = system.settings().config().getDuration("akka.routes.ask-timeout");
    }

    private CompletionStage<DataNode.Command> getAllLocalRequest() {
        return AskPattern.ask(node, ref -> new DataNode.GetAllLocalRequest(ref), askTimeout, scheduler);
    }

    private CompletionStage<DataNode.Command> getnodesRequest() {
        return AskPattern.ask(node, ref -> new DataNode.GetNodesRequest( ref), askTimeout, scheduler);
    }


    /**
     * This method creates test routes for our web app
     */
    //#test-routes
    public Route testRoutes() {
        return concat(
            //#getAllLocal
            pathPrefix("getAllLocal",  () ->
                get(() ->
                    //#answer with a getAnswer message marshalled with Jackson
                    onSuccess(getAllLocalRequest(), getAnswer -> {
                        return complete(StatusCodes.OK, getAnswer, Jackson.marshaller());
                        })
                    )
                ),
            //#getNodes
            pathPrefix("getNodes",  () ->
                get(() ->
                    //#answer with a getAnswer message marshalled with Jackson
                    onSuccess(this::getnodesRequest, getAnswer -> {
                         return complete(StatusCodes.OK, getAnswer, Jackson.marshaller());
                    })
                )
            )
        );
    }
    //#all-routes
}