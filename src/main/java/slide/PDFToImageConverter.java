package slide;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PDFToImageConverter {
    public static List<String> convertPDFToImage(String pdfFilePath, int width, int height) throws IOException {
        List<String> imagePaths = new ArrayList<>();
        File file = new File(pdfFilePath);
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); ++page) {

                BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300);

                BufferedImage resizedImage = ImageUtils.resizeImage(bim, width, height);

                String imagePath = "page_" + (page + 1) + ".jpg";
                ImageIOUtil.writeImage(resizedImage, imagePath, 300);
                imagePaths.add(imagePath);
            }
        }
        return imagePaths;
    }
}



