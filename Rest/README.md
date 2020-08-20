# REST Image Server
A simple REST image server implemented with the SparkJava framework (http://sparkjava.com/).

# Authentication
It supports user authentication with HMAC256 tokens set in the cookies. The liveness period of a token is 10 minutes. it currently does not support third party authentication. 

# APIS
| API                   | HTTP method | Body                                                     | Access  | Description  | Response ( if succeeded)                                                                                                          |
|:-----------------------:|:------:|:----------------------------------------------------------:|:---------:|:----------------------------------------------------------------------------:|---------------------------------------------|
| `/register`           | POST | `{ "username": "My_username", "password": "My_password" }` | PUBLIC | Register a user in the server | '{ "Id":"userId"} |
| `/login`           | POST | `{ "username": "My_username", "password": "My_password" }` | PUBLIC  | Login in the user in the server setting the cookies "ImageServerId" and "ImageServerToken" | 200 HTTP OK 
| `/users`              | GET  | -                                                        | PRIVATE | Get a list of all registered users | {}                                                                                        |
| `/images`             | GET  | -                                                        | PRIVATE | Returns every stored image data |-                                                                                       |
| `/users/:userId`      | GET  | -                                                        | PRIVATE | Returns user data with id `userId` |-                                                                                     |
| `/images/:imageId`    | GET  | -                                                        | PRIVATE | Returns image with id `imageId` |-                                                                                       |
| `/images/uploadImage` | POST | `{ "image" : file }`                                                        | PRIVATE | Uploads image to the server   |-                                                                                          |


