import marytts.LocalMaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;

public class VideoCreator {
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
    public static void combineImageAndAudio(String ffmpegPath, String inputImagePath, String audioFilePath, String outputVideoPath, String sentence) throws IOException, InterruptedException {
        // FFmpeg command to combine audio and image into a video
        String[] ffmpegCommand = {
                ffmpegPath,
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
}
