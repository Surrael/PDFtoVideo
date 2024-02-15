package tts;

import marytts.LocalMaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.IOException;

public class MaryTTS extends TTS {
    @Override
    public void speak(String text, String outputFilePath) {
        try {
            // Create a LocalMaryInterface
            LocalMaryInterface marytts = new LocalMaryInterface();

            // Synthesize speech from the input text
            AudioInputStream audioStream = marytts.generateAudio(text);

            AudioSystem.write(audioStream, javax.sound.sampled.AudioFileFormat.Type.WAVE, new File(outputFilePath));

        } catch (MaryConfigurationException | SynthesisException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
