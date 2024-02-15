package tts;

import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.audio.AudioSpeechRequest;
import io.github.sashirestela.openai.domain.audio.SpeechRespFmt;
import io.github.sashirestela.openai.domain.audio.Voice;

import java.io.FileOutputStream;

public class OpenAI extends TTS {
    @Override
    public void speak(String text, String outPutFilePath) {
        var openai= SimpleOpenAI.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .build();

        var speechRequest = AudioSpeechRequest.builder()
                .model("tts-1")
                .input(text)
                .voice(Voice.NOVA)
                .responseFormat(SpeechRespFmt.MP3)
                .speed(1.0)
                .build();
        var futureSpeech = openai.audios().speak(speechRequest);
        var speechResponse = futureSpeech.join();
        try {
            var audioFile = new FileOutputStream(outPutFilePath);
            audioFile.write(speechResponse.readAllBytes());
            System.out.println(audioFile.getChannel().size() + " bytes");
            audioFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
