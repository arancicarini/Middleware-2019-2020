package project;

import akka.actor.typed.ActorRef;

public class Request {
    private int counter;
    public final ActorRef<DataNode.Command> requester;

    public Request ( int counter, ActorRef<DataNode.Command> requester){
        this.counter = counter;
        this.requester = requester;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }
}
