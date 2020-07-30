import static java.lang.Integer.parseInt;
import static javax.imageio.ImageIO.read;
import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.options;
import static spark.Spark.post;
import static spark.Spark.put;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import spark.Request;
import spark.Response;

import javax.imageio.ImageIO;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.ResourceBundle;

import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Scanner;

public class SparkRestExample {
    private static String STORAGE = "storage";
    static final ServiceImplementation service= new ServiceImplementation();
    static final UserService userService = service;
    static final ImageService imageService = service;


    public static void main(String[] args) {


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
                response.cookie("ImageServerId", String.valueOf(user.getId()));

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
                String id = request.cookie("ImageServerId");
                if (token == null || id == null){
                    return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, new Gson().toJson("Missing cookies")));

                }
                userService.authenticate(token, id);
                return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(userService.getUsers())));
            }catch (UserException e){
                return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, new Gson().toJson("Invalid token")));
            }
        });

        get("/users/:id", (request, response) -> {
            response.type("application/json");
            try{
                String token = request.cookie("ImageServerToken");
                String id = request.cookie("ImageServerid");
                if (token == null || id == null){
                    return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, new Gson().toJson("Missing cookies")));

                }
                userService.authenticate(token, id);
                String id1 = request.params(":id");
                if (!id.equals(id1)){
                    return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, new Gson().toJson("You cannot spy other people")));
                }
                return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(userService.getUser(parseInt(id)))));
            }catch (UserException e){
                return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, e.getMessage()));
            }
        });

        put("/users/:id", (request, response) -> {
            response.type("application/json");
            try{
                String token = request.cookie("ImageServerToken");
                String id = request.cookie("ImageServerid");
                if (token == null || id == null){
                    return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, new Gson().toJson("Missing cookies")));

                }
                userService.authenticate(token, id);
                User toEdit = new Gson().fromJson(request.body(), User.class);
                User editedUser = userService.editUser(toEdit);
                if (editedUser != null) {
                    return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(editedUser)));
                } else {
                    return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, new Gson().toJson("User not found or error in edit")));
                }
            }catch (UserException e){
                return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, new Gson().toJson("Invalid token")));
            }
        });

        delete("/users/:id", (request, response) -> {
            response.type("application/json");
            try{
                String token = request.cookie("ImageServerToken");
                String id = request.cookie("ImageServerid");
                if (token == null || id == null){
                    return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, new Gson().toJson("Missing cookies")));

                }
                userService.authenticate(token, id);
                String id1 = request.params(":id");
                if (!id.equals(id1)){
                    return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, new Gson().toJson("You cannot delete other people accounts!")));
                }
                userService.deleteUser(parseInt(id));
                return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS, "user deleted"));
            }catch (UserException e){
                return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, new Gson().toJson("Invalid token")));
            }
        });

        options("/users/:id", (request, response) -> {
            response.type("application/json");
            try{
                String token = request.cookie("ImageServerToken");
                String id = request.cookie("ImageServerid");
                if (token == null || id == null){
                    return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, new Gson().toJson("Missing cookies")));

                }
                userService.authenticate(token, id);
                userService.deleteUser(parseInt(request.params(":id")));
                return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS, (userService.userExist(parseInt(request.params(":id"))) ? "User exists" : "User does not exists")));
            }catch (UserException e){
                return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, new Gson().toJson("Invalid token")));
            }
        });

/**************************IMAGE METHODS************************************************/

        post("/images/:id", (request, response) -> {
            response.type("application/json");
            try{
                String token = request.cookie("ImageServerToken");
                String id = request.cookie("ImageServerid");
                if (token == null || id == null){
                    return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, new Gson().toJson("Missing cookies")));

                }
                userService.authenticate(token, id);
                String key = uploadImage(request, parseInt(request.params(":id")));
                return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS,new JsonParser().parse("{\"KEY\": \""+key+"\"}")));
            }catch (UserException | ImageException e){
                return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, e.getMessage()));
            }
        });

        get("/images/:key", (request,response)->{
            try{
                String token = request.cookie("ImageServerToken");
                String id = request.cookie("ImageServerid");
                if (token == null || id == null){
                    return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, new Gson().toJson("Missing cookies")));

                }
                userService.authenticate(token, id);
            }catch ( UserException e){
                return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, e.getMessage()));
            }
            return returnImage(request,response);
        });

        get("/images/download/:key", (request,response)->{
            try{
                String token = request.cookie("ImageServerToken");
                String id = request.cookie("ImageServerid");
                if (token == null || id == null){
                    return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, new Gson().toJson("Missing cookies")));

                }
                userService.authenticate(token, id);
            }catch ( UserException e){
                return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, e.getMessage()));
            }
            return downloadImage(request,response);
        });

        get("/images/user/:id", (request, response) -> {
            response.type("application/json");
            try{
                String token = request.cookie("ImageServerToken");
                String id = request.cookie("ImageServerid");
                if (token == null || id == null){
                    return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, new Gson().toJson("Missing cookies")));

                }
                userService.authenticate(token, id);
                JsonElement list= new Gson().toJsonTree(imageService.getUserImages(parseInt(request.params(":id"))));
                System.out.println(list);
                return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS, list));
            }catch (UserException e){
                return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, e.getMessage()));
            }
        });

        delete("/images/:key/:id", (request, response) -> {
            response.type("application/json");
            try{
                String token = request.cookie("ImageServerToken");
                String id = request.cookie("ImageServerid");
                if (token == null || id == null){
                    return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, new Gson().toJson("Missing cookies")));

                }
                userService.authenticate(token, id);
                imageService.deleteImage(request.params(":key"), parseInt(request.params(":id")));
                return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS, "image deleted"));
            }catch (UserException e){
                return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, e.getMessage()));
            }
        });

    }

    public static String uploadImage(Request request, Integer id) throws ImageException {
        // TO allow for multipart file uploads
        request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(""));

        try {
            //save image in the storage
            // "file" is the key of the form data with the file itself being the value
            Part imagePart = request.raw().getPart("file");
            // The name of the file user uploaded
            String name = imagePart.getSubmittedFileName();
            InputStream stream = imagePart.getInputStream();

            //save image in the user
            Image image= new Image(name);
            String key=imageService.addImage(image,id);

            // Write stream to file under storage folder
            Path path= Paths.get(STORAGE).resolve(key);
            Files.copy(stream,path, StandardCopyOption.REPLACE_EXISTING);

            image.setPath(path);
            return key;
        } catch (IOException | ServletException e) {
            throw new ImageException("Exception occurred while uploading image");
        }
    }


    public static Response returnImage(Request request,Response response) throws ImageException {
        response.raw().setContentType("image/png");

        Path imagePath = Paths.get(STORAGE).resolve(request.params(":key"));
        File image= new File(String.valueOf(imagePath));
        try (OutputStream out = response.raw().getOutputStream()) {
            ImageIO.write(read(image), "png", out);
        } catch (IOException e) {
            throw new ImageException("Can't download the image");
        }
        return response;
    }

    public static Response downloadImage(Request request, Response response) throws IOException {
        Path imagePath = Paths.get(STORAGE).resolve(request.params(":key"));
        BufferedImage image = read(new File(String.valueOf(imagePath)));
        HttpServletResponse raw = null;
        try {
            raw = response.raw();
            OutputStream out = raw.getOutputStream();
            response.header("Content-Disposition", "attachment; filename=image.png");
            ImageIO.write(image, "png", out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }
}