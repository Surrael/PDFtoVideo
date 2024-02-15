package video;

import slide.PDFToImageConverter;
import threading.TaskThreadPool;
import threading.VideoBuilderTask;
import marytts.LocalMaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import tts.MaryTTS;
import tts.OpenAI;

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

    public static void generateVideo(File script, String pdfFilePath,
                                     String outputFileName,  Map<Integer, List<String>> scriptMap) throws IOException {

        if (scriptMap == null) scriptMap = ScriptParser.parseScriptFile(script);

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
            // For each line in a given slide, we submit the Threading.VideoBuilderTask
            for (int id = 0; id < slideLines.size(); id++) {
                int taskID = (slideNumber * maxSlideLines) + id;
                pool.submitTask(new VideoBuilderTask(taskID, slideLines.get(id), images.get(slideNumber), new MaryTTS())); // Pick TTS
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

        /* This allows you to overlay better TTS audio but might cause desync in subtitles
        // Combine all strings into one long string
        StringBuilder combinedString = new StringBuilder();
        for (List<String> strings : scriptMap.values()) {
            for (String str : strings) {
                combinedString.append(str).append(" "); // Add each string followed by a space
            }
        }

        // Remove trailing space
        if (combinedString.length() > 0) {
            combinedString.deleteCharAt(combinedString.length() - 1);
        }

        // Overlay better TTS audio
        (new OpenAI()).speak(combinedString.toString(), "finalaudio.mp3");

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffmpeg",
                    "-y",
                    "-i", "temp.mp4",
                    "-i", "finalaudio.mp3",
                    "-c:v", "copy",
                    "-c:a", "aac",
                    "-strict", "experimental",
                    "-map", "0:v:0",
                    "-map", "1:a:0",
                    "-shortest",
                    outputFileName + ".mp4"
            );
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Audio replacement successful");
            } else {
                System.out.println("Error: Audio replacement failed");
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        File finalAudio = new File("finalaudio.mp3");
        File tempVideo = new File("temp.mp4");
        finalAudio.delete();
        tempVideo.delete();
        */

        deleteTempFiles("inputconcat.txt", images, scriptMap, maxSlideLines);
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
        File concatenatedInput = new File(inputconcat);
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
