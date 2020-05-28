## How to build the AkkaProject
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
API | Parameters |  Meaning
------------ | ------------- | ------------- 
/get/?key=$id| a String Key| Get the value associated with the specified Key.
/put/?key=$keyId&value=$valueId | a String KeyId and a String ValueId | Insert into the cluster the specified Value associated with the specified Key.
/getAllLocal | none | get all the values locally stored in the contacted node.
/getNodes | none | get a representation of all nodes currently up in the cluster.


## Common issues.
- if the project does not build, try
    - mvn clean package install
    - mvn clean install

