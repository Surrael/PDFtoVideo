package slide;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.xslf.usermodel.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

public class PPTXextractor {
    // Returns an array of sentences
    // Path to the OpenNLP sentence model file
    private static final String SENTENCE_MODEL_PATH = "lib/en-sent.bin";

    public static Map<Integer, List<String>> fetchNotes(String path) {
        Map<Integer, List<String>> notesMap = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(path)) {
            XMLSlideShow ppt = new XMLSlideShow(fis);
            List<XSLFSlide> slides = ppt.getSlides();

            // Load OpenNLP sentence model
            InputStream modelIn = new FileInputStream(SENTENCE_MODEL_PATH);
            SentenceModel model = new SentenceModel(modelIn);
            SentenceDetectorME sentenceDetector = new SentenceDetectorME(model);

            for (int i = 0; i < slides.size(); i++) {
                XSLFSlide slide = slides.get(i);
                List<String> sentences = new ArrayList<>();

                try {
                    XSLFNotes mynotes = slide.getNotes();
                    for (XSLFShape shape : mynotes) {
                        if (shape instanceof XSLFTextShape && Placeholder.BODY == ((XSLFTextShape) shape).getTextType()) {
                            XSLFTextShape txShape = (XSLFTextShape) shape;
                            // Split text into sentences
                            String[] sentencesArray = sentenceDetector.sentDetect(txShape.getText());
                            sentences.addAll(Arrays.asList(sentencesArray));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                notesMap.put(i, sentences);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return notesMap;
    }

    public static void convertPptxToPdf(String pptxFilePath, String pdfFilePath) throws IOException {
        // Load .pptx file
        XMLSlideShow ppt = new XMLSlideShow(new FileInputStream(pptxFilePath));
        // Create a new PDF document
        PDDocument doc = new PDDocument();
        // For each slide in the .pptx file
        for (XSLFSlide slide : ppt.getSlides()) {
            // Create a new page in the PDF document
            PDPage page = new PDPage(new PDRectangle((float) ppt.getPageSize().getWidth(), (float) ppt.getPageSize().getHeight()));
            doc.addPage(page);
            // Create a new content stream that writes to the PDF document
            PDPageContentStream content = new PDPageContentStream(doc, page);
            // Convert the slide to an image
            int scale = 4; // Increase this value to increase the resolution
            BufferedImage img = new BufferedImage(scale * (int) ppt.getPageSize().getWidth(), scale * (int) ppt.getPageSize().getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = img.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            graphics.scale(scale, scale);
            slide.draw(graphics);
            // Convert the image to a PDImageXObject
            PDImageXObject ximage = PDImageXObject.createFromByteArray(doc, toByteArrayAutoClosable(img, "png"), "slide");
            // Draw the image on the PDF page
            content.drawImage(ximage, 0, 0, page.getMediaBox().getWidth(), page.getMediaBox().getHeight());
            // Close the content stream
            content.close();
        }
        // Save the PDF document to a file
        doc.save(pdfFilePath);
        // Close the PDF document
        doc.close();
    }

    private static byte[] toByteArrayAutoClosable(BufferedImage img, String formatName) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, formatName, out);
            return out.toByteArray();
        }
    }

}
