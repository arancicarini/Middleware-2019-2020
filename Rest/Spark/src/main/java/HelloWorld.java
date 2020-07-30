import spark.Request;
import spark.utils.IOUtils;

import javax.imageio.ImageIO;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.stream.Collectors;

import static javax.imageio.ImageIO.read;
import static spark.Spark.*;

public class HelloWorld{
    private static String STORAGE = "storage";

    public static void main(String[] args) {
        File storageDir = new File(STORAGE);
        if (!storageDir.isDirectory()) storageDir.mkdir();

        /**File uploadDir = new File("upload");
        uploadDir.mkdir(); // create the upload directory if it doesn't exist
        //staticFiles.location("/public"); // Static files
        staticFiles.externalLocation("upload");**/

        post("/upload", (req, res) -> uploadFile(req.params(":user"),req));
        get("/download/:file", (req, res) -> downloadFile(req.params(":file")));
        get("/count", (req, res) -> countFiles());
        delete("/delete/:file", (req, res) -> deleteFile(req.params(":file")));
        get("/hello", (req, res)->"Hello, world");

        /*get("/hello/:name", (req,res)->{
            return "Hello, "+ req.params(":name");
        });*/

        get("/download/image/:file", (req, res) ->{
            res.raw().setContentType("image/png");

            Path filePath = Paths.get(STORAGE).resolve(req.params(":file"));
            File file= new File(String.valueOf(filePath));

            try (OutputStream out = res.raw().getOutputStream()) {
                ImageIO.write(read(file), "png", out);
            }
            return res;
        });




    }



    private static String uploadFile(String user, Request request) {
        // TO allow for multipart file uploads
        request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(""));

        try {
            // "file" is the key of the form data with the file itself being the value
            Part filePart = request.raw().getPart("file");
            // The name of the file user uploaded
            String uploadedFileName = filePart.getSubmittedFileName();
            InputStream stream = filePart.getInputStream();

            // Write stream to file under storage folder
            Files.copy(stream, Paths.get(STORAGE).resolve(uploadedFileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | ServletException e) {
            return "Exception occurred while uploading file" + e.getMessage();
        }
        return "File successfully uploaded by ";
    }

    private static String downloadFile(String fileName) {
        Path filePath = Paths.get(STORAGE).resolve(fileName);
        File file = filePath.toFile();
        if (file.exists()) {
            try {
                // Read from file and join all the lines into a string
                return String.join("", Files.readAllLines(filePath));
            } catch (IOException e) {
                return "Exception occurred while reading file" + e.getMessage();
            }
        }
        return "File doesn't exist. Cannot download";
    }


    private static int countFiles() {
        // Count the number of files in the storage folder
        return new File(STORAGE).listFiles().length;
    }

    private static String deleteFile(String fileName) {
        File file = Paths.get(STORAGE).resolve(fileName).toFile();
        if (file.exists()) {
            file.delete();
            return "File deleted";
        } else {
            return "File " + fileName + " doesn't exist";
        }
    }

}
