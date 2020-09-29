[![](https://img.shields.io/maven-central/v/com.typesafe.akka/akka.svg)](https://mvnrepository.com/artifact/com.typesafe.akka/akka-actor)
![Akka](https://github.com/arancicarini/Middleware-2019-2020/workflows/Akka/badge.svg)
# Akka dictionary server
A simple distributed dictionary server implemented with the Akka toolkit (https://akka.io/), which enforces replication of data among cluster nodes to improve rubustness. Keys and values are of String type.

The number of replicas can be chosen modifying the variable akka.replicas in AkkaProject/src/main/resources/application.conf ( default is 1)

A reference to all the Akka APIs can be found at:
https://doc.akka.io/japi/akka/current/index.html?akka/cluster/ddata/typed/javadsl/package-summary.html&_ga=2.137087859.212732556.1586383315-309149511.1585228667

## How to build and test the AkkaProject
- Digit http://192.168.1.1/login.lp on your web browser ( or equivalent Ip address to enter your Router configuration interface).
- Detect the IP address of your device in your LAN (default: 192.168.1.3).
- If different from the default address, 
    - Open the project in Intellij
    - Open src/main/resources/application.conf
    - edit akka.cluster.seed-nodes with your custom IP address
- If not already done,
    - Download and install Maven from https://maven.apache.org/.
    - Don't forget to add  <path_to_Maven>/Maven<version_of_Maven>/bin  to your env variables under the PATH variable.
- open a shell in the AkkaProject folder (hereunder called simply "root").
    - type **mvn compile**
    - type **mvn exec:java -Dexec.mainClass="project.App" -Dexec.args=25251**
    - wait until logged "Member is up!"
- open another shell in the root folder
    - type **mvn exec:java -Dexec.mainClass="project.App" -Dexec.args=25252**
    - wait until logged "Member is up!"
- these are the seed nodes of the cluster: if both are down, all the cluster is down
- open other shells (possibly on other devices in the same LAN) and create new nodes with the same command
    - note: the couple <IP_Address, Port> is unique for each node, so you can't create two nodes with the same port on the same device  

## APIS 
Each node of the cluster has exactly the same role among all its peers: they run the same actors with the same code. There is no single point of failure for the service. The only exception to this is the presence in Akka of seed nodes: seed nodes are declared in the config file, and at least one of the seed nodes must be up to guarantee the availability of the service. Therefore, each node exposes the same APIs.

All APIs responses are of type application/json. The body of the requests must be of type Json. Both the POST and the GET methods return a JSON with field (among the others) requestId. This is just an internal identifier of the request. 

| API                   | HTTP method | Request Body                                              | Description  | Response ( if successful) |
|:--------------------:|:--------:|:---------------------------------------------------------------:|:------------------------------------------------------------------------------:|:------------------------------|
|`/dictionary` | POST | `{ "key":"MyKey", "value":"MyValue" }`                            | Insert an entry key - value into the dictionary | `{"requestId": "a-number","success": true}` |
| `/dictionary/:key` | GET | -                                                         | Return the value associated with `key` | `{"isPresent": true,"key": "MyKey","requestId": a_number,"value": "myValue"}`|
| `/test/localData` | GET | -                                                         | Return all the values stored locally in the contacted node | `"values": [ "MyValue1", "MyValue2", ... ]` |
| `/test/nodes` | GET | -                                                         | Return a representation of all nodes currently up in the cluster | `"nodes": [{ "hashKey":"hash1", "node": {"local": false,"terminated": false}}, { "hashKey": "hash2","node": {"local": false, "terminated": false }}, { "hashKey": "hash3",  "node": { "local": true, "terminated": false}}]` |

## Main features
- full distribution
- K fault tolerance: upon a failure of a node, its data are replicated to another node
- Client centric consistency: synchronous writes
- Support for dynamic insertion of nodes into the cluster
- REST APIs to interact with the cluster
- Each data has a leader replica which solves W/W conflicts
## Common issues.
- if the project does not build, try
    - mvn clean package install
    - mvn clean install
