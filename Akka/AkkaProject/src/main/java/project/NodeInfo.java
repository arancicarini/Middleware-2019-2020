package project;

import akka.actor.typed.ActorRef;

public class NodeInfo {
    public final String hashKey;
    public final ActorRef<DataNode.Command> node;

    public NodeInfo(String hashKey, ActorRef<DataNode.Command> node) {
        this.hashKey = hashKey;
        this.node = node;
    }

    public String getHashKey() {
        return hashKey;
    }

    public ActorRef<DataNode.Command> getNode() {
        return node;
    }

}
