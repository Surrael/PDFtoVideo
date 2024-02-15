package threading;

import tts.TTS;
import video.VideoCreator;

import java.io.File;
import java.io.PrintWriter;

public class VideoBuilderTask extends Task<VideoBuilderTask, String> {

    private final String script;
    private final String image;
    private final TTS tts;

    public VideoBuilderTask(int id, String script, String image, TTS tts) {
        super(id);
        this.script = script;
        this.image = image;
        this.tts = tts;
    }

    @Override
    public String runTask() throws Exception {
        File audioFile= new File("audio" + this.getId() + ".mp3");
        tts.speak(script, audioFile.getName());
        File file = new File(String.format("sub%d.srt", this.getId()));
        try(PrintWriter fw = new PrintWriter(file)) {
            fw.println("1");
            fw.println("00:00:00,000 --> 99:59:59,999");
            fw.print(script);
        } finally {
            VideoCreator.combineImageAndAudio(image,
                    "audio" + this.getId() + ".mp3", "output" + this.getId() + ".mp4",
                    "sub" + this.getId() + ".srt");
            file.delete();
            audioFile.delete();
        }
        return String.format("output%d.mp4", this.getId());
    }

}