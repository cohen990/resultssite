package com.hackingdan;

import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;
import spark.utils.IOUtils;

import javax.imageio.ImageIO;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.*;

public class Site {

    private static final String EXPERIMENTS_PATH = "/target/classes/experiments/";

    public static void main(String[] args) {
        port(5000);
        makeIfNotExists(getAbsolutePath());
        staticFiles.externalLocation(getAbsolutePath());

        get("/", (req, res) -> {
            List<String> experiments = getExperimentNames();
            Map<String, Object> model = new HashMap<>();
            model.put("experiments", experiments);
            return new VelocityTemplateEngine().render(
                    new ModelAndView(model, "experiments.vm")
            );
        });

        get("/results", (req, res) -> {
            String experimentName = req.queryParams("experiment");
            Map<String, Object> model = new HashMap<>();
            model.put("experiment", experimentName);
            return new VelocityTemplateEngine().render(
                    new ModelAndView(model, "results.vm")
            );
        });

        post("/results", (req, res) -> {
            req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
            Part experiment = req.raw().getPart("experiment");
            Part image = req.raw().getPart("results");
            String experimentName = IOUtils.toString(experiment.getInputStream());
            writeImage(image, experimentName);
            res.status(201);
            return "";
        });

        get("/health", (req, res) -> {
            res.status(200);
            return "";
        });
    }

    private static List<String> getExperimentNames() {
        File folder = new File(getAbsolutePath());
        File[] listOfFiles = folder.listFiles();
        List<String> names = extractFileNames(listOfFiles);
        return names;
    }

    private static List<String> extractFileNames(File[] listOfFiles) {
        List<String> names = new ArrayList<>();
        if(listOfFiles == null){
            return names;
        }
        for (File file:listOfFiles) {
            if(file.isFile() && !file.isHidden()) {
                names.add(file.getName());
            }
        }

        return names;
    }

    private static void writeImage(Part imagePart, String fileName) {
        try {

            InputStream is = imagePart.getInputStream();
            Image image = ImageIO.read(is);

            BufferedImage bufferedImage = createResizedCopy(image, 684, 662, true);
            ImageIO.write(bufferedImage, "png", new File(getAbsolutePath() + fileName + ".png"));


        } catch (IOException e) {
            System.out.println("Error");
            e.printStackTrace();
        }}

    private static void makeIfNotExists(String path) {
        File directory = new File(path);
        if (!directory.exists()){
            directory.mkdirs();
        }
    }

    static BufferedImage createResizedCopy(Image originalImage, int scaledWidth, int scaledHeight, boolean preserveAlpha) {
            int imageType = preserveAlpha ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
            BufferedImage bufferedImage = new BufferedImage(scaledWidth, scaledHeight, imageType);
            Graphics2D graphics = bufferedImage.createGraphics();
            if (preserveAlpha) {
                graphics.setComposite(AlphaComposite.Src);
            }
            graphics.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
            graphics.dispose();
            return bufferedImage;
        }

    public static String getAbsolutePath() {
        return System.getProperty("user.dir") + EXPERIMENTS_PATH;
    }
}
