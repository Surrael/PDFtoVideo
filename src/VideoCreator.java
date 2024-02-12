import Threading.TaskThreadPool;
import marytts.LocalMaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class VideoCreator {
    private final static int THREADS = 10;
    private final static int WIDTH = 1920;
    private final static int HEIGHT = 1080;

    public static void generateVideo(File script, String pdfFilePath, String outputFileName) throws IOException {
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
        VideoCreator.concatenateMP4Files("inputconcat.txt", outputFileName + ".mp4");

        deleteTempFiles("inputconcat.txt", images, scriptMap, maxSlideLines);
    }
    public static void convertTextToSpeech(String inputText, String outputFilePath) {
        try {
            // Create a LocalMaryInterface
            LocalMaryInterface marytts = new LocalMaryInterface();

            // Synthesize speech from the input text
            AudioInputStream audioStream = marytts.generateAudio(inputText);

            AudioSystem.write(audioStream, javax.sound.sampled.AudioFileFormat.Type.WAVE, new File(outputFilePath));

        } catch (MaryConfigurationException | SynthesisException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void combineImageAndAudio(String inputImagePath, String audioFilePath, String outputVideoPath, String sentence) throws IOException, InterruptedException {
        // FFmpeg command to combine audio and image into a video
        String[] ffmpegCommand = {
                "ffmpeg",
                "-y",
                "-loop", "1",
                "-i", inputImagePath,
                "-i", audioFilePath,
                "-vf", "subtitles='" + sentence + "'", // Subtitle text is dynamically included from the variable
                "-c:v", "libx264",
                "-tune", "stillimage",
                "-preset", "ultrafast",
                "-crf", "30",
                "-level:v", "3.0",
                "-c:a", "aac",
                "-strict", "experimental",
                "-shortest",
                outputVideoPath
        };


        // Execute FFmpeg command
        ProcessBuilder processBuilder = new ProcessBuilder(ffmpegCommand);
        processBuilder.inheritIO(); // Redirects the process's standard output and error to the Java process
        Process process = processBuilder.start();
        process.waitFor(); // Wait for FFmpeg to finish
    }

    public static void concatenateMP4Files(String input, String outputFilePath) {
        try {
            // Prepare FFmpeg command
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffmpeg",
                    "-y",
                    "-f", "concat",
                    "-i", input,
                    "-c", "copy",
                    outputFilePath
            );

            // Redirect error stream to output stream
            processBuilder.redirectErrorStream(true);

            // Start the FFmpeg process
            Process process = processBuilder.start();

            // Read the output of FFmpeg command
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // Wait for the process to finish
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("MP4 files concatenated successfully.");
            } else {
                System.out.println("Failed to concatenate MP4 files. FFmpeg process exited with error code " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
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
