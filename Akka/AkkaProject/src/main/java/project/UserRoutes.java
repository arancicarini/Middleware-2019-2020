package project;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Scheduler;
import akka.actor.typed.javadsl.AskPattern;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.PathMatchers;
import akka.http.javadsl.server.Route;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
        return AskPattern.ask(node, ref -> new DataNode.PutRequest(key, new Value(value, -1), ref), askTimeout, scheduler);
    }


    /**
     * This method creates all the user routes of our web app
     */
    //#all-routes
    public Route userRoutes() {
        return pathPrefix("dictionary",  () ->
            concat(
                pathEnd( () ->
                    post(() -> entity( Jackson.unmarshaller(DictionaryEntry.class), request ->
                        //#answer with a putAnswer message marshalled with Jackson
                        onSuccess(putRequest(request.key, request.value), putAnswer -> {
                            return complete(StatusCodes.OK, putAnswer, Jackson.marshaller());
                        }))
                    )
                ),
                path(PathMatchers.segment(), (String key) ->
                    get(() ->
                        //#answer with a getAnswer message marshalled with Jackson
                        onSuccess(getRequest(key), getAnswer -> {
                        return complete(StatusCodes.OK, getAnswer, Jackson.marshaller());
                        }))
                )
            )
        );
    }
    //#all-routes

    public final static class DictionaryEntry{
        public final String key;
        public final String value;

        @JsonCreator
        public DictionaryEntry(@JsonProperty("key")String key,@JsonProperty("value") String value){
            this.key = key;
            this.value = value;
        }
    }






}

