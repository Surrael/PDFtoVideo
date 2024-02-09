import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptParser {
    public static Map<Integer, List<String>> parseScriptFile(File scriptFile) throws IOException {
        List<String> lines = Files.readAllLines(scriptFile.toPath());
        
        Map<Integer, List<String>> scriptMap = new HashMap<Integer, List<String>>();
        List<String> slideLines = new ArrayList<String>();
        int slide = 0;
        boolean reading = false;

        for(String line : lines) {
            if(line.equals("")) continue;
            if(line.startsWith("#BEGIN-SLIDE:") && !reading) {
                slideLines = new ArrayList<String>();
                slide = Integer.parseInt(line.substring("#BEGIN-SLIDE:".length()));
                reading = true;
            } else if(reading) {
                if(line.startsWith("#BEGIN-SLIDE:")) {
                    throw new RuntimeException("Incorrect script format input");
                } else if(line.startsWith("#END-SLIDE")) {
                   reading = false;
                   scriptMap.put(slide, slideLines);
                   slideLines = new ArrayList<String>();
                } else {
                    slideLines.add(line);
                }
            } else {
                throw new RuntimeException("Incorrect script format input");
            }
        }
        return scriptMap;
    }
}