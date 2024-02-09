import Threading.TaskThreadPool;
import marytts.exceptions.MaryConfigurationException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Main {
    private final static int THREADS = 10;
    private final static String pdfFilePath = "testslides.pdf";
    private final static File script = new File("script.txt");

    private final static int WIDTH = 1920;
    private final static int HEIGHT = 1080;
    public static void main(String[] args) throws IOException, InterruptedException, MaryConfigurationException {
        
        // Parse the script
        Map<Integer, List<String>> scriptMap = ScriptParser.parseScriptFile(script);

        // Convert slides to images and fetch their paths
        List<String> images = PDFToImageConverter.convertPDFToImage(pdfFilePath, WIDTH, HEIGHT);

        // Used to save the paths to the mp4 videos per sentence in chronological order
        Map<Integer, String> mp4FilePaths = Collections.synchronizedSortedMap(new TreeMap<>());
        
        // Maximum amount of slides in any slide
        int maxSlideLines = scriptMap.values().stream().min((a, b) -> Integer.compare(b.size(), a.size())).get().size();

        // Create mp4s for each sentence with their respective slides, using multithreading
        scriptMap.forEach((slideNumber, slideLines) -> {
            // Build a thread pool of size THREADS
            TaskThreadPool<VideoBuilderTask, String> pool = TaskThreadPool.createPool(THREADS);
            pool.setFinishListener((task, mp4FilePath)->{
                mp4FilePaths.put(task.getId(), mp4FilePath);
            });
            // For each line in a given slide, we submit the VideoBuilderTask
            for (int id = 0; id < slideLines.size(); id++) {
                int taskID = (slideNumber * maxSlideLines) + id;
                pool.submitTask(new VideoBuilderTask(taskID, slideLines.get(id), images.get(slideNumber)));
            }
            pool.initShutdown(true); // synchronous

            // Write the mp4 paths to inputconcat.txt to be concatenated later
            File file = new File("inputconcat.txt");
            try(FileWriter fw = new FileWriter(file)) {
                mp4FilePaths.forEach((k, v)->{
                    try {
                        fw.write("file '" + v + "'\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // Concatenate all sentences
        VideoCreator.concatenateMP4Files("inputconcat.txt", "outputconcat.mp4");

        deleteTempFiles("inputconcat.txt", images, scriptMap, maxSlideLines);

        System.exit(0);
    }

    private static void deleteTempFiles(String inputconcat, List<String> images, Map<Integer, List<String>> scriptMap, int maxSlideLines) {
        // Delete temp input list
        File concatenatedInput = new File("inputconcat.txt");
        concatenatedInput.delete();


        // Delete temp images
        for (String image : images) {
            File imageFile = new File(image);
            imageFile.delete();
        }

        // Delete temp mp4 files
        scriptMap.forEach((slideNumber, slideLines) -> {
            for (int id = 0; id < slideLines.size(); id++) {
                int taskID = (slideNumber * maxSlideLines) + id;
                File intermediateOutput = new File("output" + taskID + ".mp4");
                intermediateOutput.delete();
            }
        });
    }
}