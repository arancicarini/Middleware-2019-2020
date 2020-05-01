## Official Akka link
https://akka.io/

## Akka APIS
https://doc.akka.io/japi/akka/current/index.html?akka/cluster/ddata/typed/javadsl/package-summary.html&_ga=2.137087859.212732556.1586383315-309149511.1585228667

## How to build and test the akka-sample-cluster-java-customer
- Digit http://192.168.1.1/login.lp on your web browser ( or equivalent Ip address to enter your Router configuration interface).
- Detect the IP address of your device in your LAN (default: 192.168.1.3).
- If different from the default address, 
    - Open the project in Intellij
    - Open src/main/resources/application.conf
    - edit akka.cluster.seed-nodes with your custom IP address
- If not already done,
    - Download and install Maven from https://maven.apache.org/.
    - Don't forget to add  <path_to_Maven>/Maven <version> /bin  to your env variables under the PATH variable.
- open a shell in the akka-sample-cluster-java-customer folder (hereunder called simply "root").
    - type **mvn compile**
    - type **mvn exec:java -Dexec.mainClass="sample.cluster.simple.App" -Dexec.args=25251**
    - wait until logged "Member is up!"
- open another shell in the root folder
    - type **mvn exec:java -Dexec.mainClass="sample.cluster.simple.App" -Dexec.args=25252**
    - wait until logged "Member is up!"
- open other shells in the root folder and start new nodes at your pleasure, changing the port of the node
    - e.g. **mvn exec:java -Dexec.mainClass="sample.cluster.simple.App" -Dexec.args=25253**
- open a shell which supports CURL commands (I personally use Git bash)
    - type **curl http://localhost:25251/greet/Arianna**
    - (of course you can contact any other running node changing the value of the port)
    - look at the result in the corresponding shell which is running the contacted node.
    
- Still to be done: nodes which contact each other (the "Say Hello" message)
    


## Common issues.
- if the project does not build, try
    - mvn clean package install
    - mvn clean install


