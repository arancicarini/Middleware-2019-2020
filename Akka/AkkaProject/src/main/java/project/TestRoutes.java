package project;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Scheduler;
import akka.actor.typed.javadsl.AskPattern;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.Route;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

import static akka.http.javadsl.server.Directives.*;

/**
 * This is a separate class used to define routes, the API endpoints
 */
//#test-routes-class
public class TestRoutes {
    //#user-routes-class
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
    public Route testRoutes() {
        return
            pathPrefix("test",  () ->
                concat(
                    pathPrefix("localData", () ->
                        pathEnd( () ->
                            get(() ->
                                onSuccess(this::getAllLocalRequest, getAnswer -> {
                                    return complete(StatusCodes.OK, getAnswer, Jackson.marshaller());
                                })
                            )
                        )
                    ),
                    pathPrefix("nodes", () ->
                        pathEnd( () ->
                            get(() ->
                                onSuccess(this::getnodesRequest, getAnswer -> {
                                    return complete(StatusCodes.OK, getAnswer, Jackson.marshaller());
                                })
                            )
                        )
                    )
                )
        );
    }
    //#test-routes
}