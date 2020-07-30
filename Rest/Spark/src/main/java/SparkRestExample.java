import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.options;
import static spark.Spark.post;
import static spark.Spark.put;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Scanner;

public class SparkRestExample {

    public static void main(String[] args) {
        final UserService userService = new ServiceImplementation();
        final ImageService imageService = new ServiceImplementation();
/****************************  authentication methods **********************************************/

        post("/register", (request, response) -> {
            response.type("application/json");

            User user = new Gson().fromJson(request.body(), User.class);
            try {
                Integer userId = userService.registerUser(user);
                return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS,new JsonParser().parse("{\"ID\": \""+String.valueOf(userId)+"\"}")));
            }catch (UserException e){
                return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, new Gson().toJson("Missing required parameters")));
            }

        });

        post("/login", (request, response) -> {
            User user = new Gson().fromJson(request.body(), User.class);
            try {
                String token  = userService.login(user);
                response.cookie("ImageServerToken", token);
                response.cookie("ImageServerUsername", user.getUsername());
                return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS));
            }catch (UserException e){
                return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, new Gson().toJson("Missing required parameters")));
            }
        });

/**************************************************************************/

        get("/users", (request, response) -> {
            response.type("application/json");
            try{
                String token = request.cookie("ImageServerToken");
                String username = request.cookie("ImageServerUsername");
                userService.authenticate(token, username);
                return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(userService.getUsers())));
            }catch (UserException e){
                return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, new Gson().toJson("Invalid token")));
            }
        });

        get("/users/:id", (request, response) -> {
            response.type("application/json");

            return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(userService.getUser(Integer.parseInt(request.params(":id"))))));
        });

        put("/users/:id", (request, response) -> {
            response.type("application/json");

            User toEdit = new Gson().fromJson(request.body(), User.class);
            User editedUser = userService.editUser(toEdit);

            if (editedUser != null) {
                return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(editedUser)));
            } else {
                return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, new Gson().toJson("User not found or error in edit")));
            }
        });

        delete("/users/:id", (request, response) -> {
            response.type("application/json");

            userService.deleteUser(Integer.parseInt(request.params(":id")));
            return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS, "user deleted"));
        });

        options("/users/:id", (request, response) -> {
            response.type("application/json");

            return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS, (userService.userExist(Integer.parseInt(request.params(":id"))) ? "User exists" : "User does not exists")));
        });

/**************************************************************************/

        post("/images", (request, response) -> {
            response.type("application/json");

            Image image = new Gson().fromJson(request.body(), Image.class);
            imageService.addImage(image,Integer.parseInt(request.params(":id")));

            return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS,new JsonParser().parse("{\"KEY\": \""+image.getKey()+"\"}")));
        });

        get("/images/:key", (request, response) -> {
            response.type("application/json");

            return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(imageService.getImage(request.params(":key")))));
        });

        get("/images/:id", (request, response) -> {
            response.type("application/json");

            return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(imageService.getUserImages(Integer.parseInt(request.params(":id"))))));
        });



    }


}