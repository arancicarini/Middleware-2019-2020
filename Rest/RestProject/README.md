[![](https://img.shields.io/maven-central/v/com.sparkjava/spark-core.svg)](http://mvnrepository.com/artifact/com.sparkjava/spark-core)
![Rest](https://github.com/arancicarini/Middleware-2019-2020/workflows/Rest/badge.svg)
## REST Image Server
A simple REST image server implemented with the SparkJava framework (http://sparkjava.com/). Images must be in .png format.

<p align="center">
  <img src="https://upload.wikimedia.org/wikipedia/commons/7/7a/Spark_Java_Logo.png" width="35%" height="150">
</p>


## Security policy
The login API generates a HMAC256 token from a secret and the user id, using a Java implementation of JWT available at https://github.com/auth0/java-jwt. This token is set in the cookie "ImageServerToken", and together with the cookie "ImageServerId", which contains the user id, enforces user authentication to access private APIs. The liveness period of a token is 10 minutes. The server currently does not support third party authentication.


## APIS
All APIs responses are of type application/json and return a status of the request ( ERROR or SUCCESS), a message explaining the error in case of error and possibly some data in case of success, apart from `/images/:key`, which returns a response of type image/png and `/images/download/:key`, which returns a raw response. The body of the request must be of type Json, apart from `/images` (POST).
| API                   | HTTP method | Body                                                         | Access  | Description  | Response data ( if STATUS == SUCCESS and response.type = application/json )                                                                                                         |
|:-----------------------:|:------:|:----------------------------------------------------------:|:---------:|:---------------------------------------------------------:|---------------------------------------------------------|
| `/register`           | POST | `{ "username": "My_username", "password": "My_password" }` | PUBLIC | Register a user in the server | `{ "Id":"userId"}` |
| `/login`           | POST | `{ "username": "My_username", "password": "My_password" }` | PUBLIC  | Login in the user in the server setting the cookies "ImageServerId" and "ImageServerToken" | - 
| `/users`              | GET  | -                                                        | PRIVATE | Get a list of all registered users |  `[ "My_Username1", "My_Username2", ...]`  |                                                                                      |
| `/users/:id`      | GET  | -                                                        | PRIVATE | Return all data of the user with id `id`, if the same is authenticated |  `{ "id":id , "username": "My_Username", "password": "Password_hash", "images": { [ image1, image2, ...]}, "counter": #images }` |
| `/users/:id`      | DELETE | -                                                        | PRIVATE | Delete all data of the user with id `id`, if the same is authenticated | - |
| `/images` | POST | (as form data, the file itself is the data)`{ "file" : file }`                                                        | PRIVATE | Upload an image to the server, return the key of the image   | `{"key":"imageKey"}` |
| `/images/:key` | GET | -                                                        | PRIVATE |Return the image associated with `key` in the user account   | the image |
| `/images/download/:key` | GET | -                                                        | PRIVATE |Download in the user device the  image associated with `key` in the user account   | - |
| `/images` | GET | -                                                        | PRIVATE |Return the descriptions of all the images associated with the user account, including a link per each image   | `[ { "key": 0, "title": "imageTitle", "path": "http://localhost:4567/images/0"}, { "key": 1, "title": "imageTitle1", "path": "http://localhost:4567/images/1"}, ...]` |
| `/images/:key` | DELETE | -                                                        | PRIVATE |Delete the image associated with `key` in the user account   | - |

## How to test the server
Open a shell and navigate to RestProject in the cloned repository (of course you need to have a jdk and maven installed in your shell). Type:

    - mvn compile
    
    - mvn exec:java -Dexec.mainClass="App"
    
The server is now working at http://localhost:4567.
Test it easily using http://restclient.net/ for Firefox or https://www.postman.com/ for Chrome.
                                                                                                                                                                              |



