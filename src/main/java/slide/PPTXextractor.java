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

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    public static void convertPptxToPdf(String pptxFilePath, String pdfFilePath) {
        try (FileInputStream fis = new FileInputStream(pptxFilePath);
             FileOutputStream fos = new FileOutputStream(pdfFilePath);
             XMLSlideShow ppt = new XMLSlideShow(fis)) {

            // Create a PDF document
            PDDocument document = new PDDocument();

            // Get the dimensions of the slide
            Dimension slideSize = ppt.getPageSize();

            // Iterate over each slide in the PowerPoint file
            for (XSLFSlide slide : ppt.getSlides()) {
                // Create a new page in the PDF document for each slide
                PDPage page = new PDPage(new PDRectangle((float)slideSize.getWidth(), (float)slideSize.getHeight()));
                document.addPage(page);

                // Create a content stream for the page
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    // Create a BufferedImage to render the slide content
                    BufferedImage img = new BufferedImage((int)slideSize.getWidth(), (int)slideSize.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = img.createGraphics();

                    // Paint the slide background
                    graphics.setPaint(Color.white);
                    graphics.fill(new Rectangle2D.Float(0, 0, (float)slideSize.getWidth(), (float)slideSize.getHeight()));

                    // Render the slide content
                    slide.draw(graphics);

                    // Convert the BufferedImage to a PDImageXObject
                    PDImageXObject pdImage = LosslessFactory.createFromImage(document, img);

                    // Draw the PDImageXObject onto the PDF page
                    contentStream.drawImage(pdImage, 0, 0);
                }
            }

            // Save the PDF document
            document.save(fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
