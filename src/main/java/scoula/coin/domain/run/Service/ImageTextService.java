package scoula.coin.domain.run.Service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ImageTextService {
    public byte[] addTextToImage(byte[] originalImage, LocalDateTime dateTime, String location) throws IOException {
        // Convert byte array to BufferedImage
        ByteArrayInputStream bais = new ByteArrayInputStream(originalImage);
        BufferedImage image = ImageIO.read(bais);

        // Create a copy of the image
        BufferedImage newImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        // Get graphics context
        Graphics2D g2d = newImage.createGraphics();

        // Copy original image
        g2d.drawImage(image, 0, 0, null);

        // Configure text rendering
        g2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );

        // Set font
        Font font = new Font("Arial", Font.BOLD, 50);
        g2d.setFont(font);
        g2d.setColor(Color.WHITE);

        // Format date
        String dateText = dateTime.format(
                DateTimeFormatter.ofPattern("yyyy.MM.dd (HH:mm)")
        );

        // Calculate text positions
        FontMetrics metrics = g2d.getFontMetrics(font);
        int dateX = (image.getWidth() - metrics.stringWidth(dateText)) / 2;
        int dateY = (image.getHeight() / 2) + 250;  // Adjust Y position as needed

        int locationX = (image.getWidth() - metrics.stringWidth(location)) / 2;
        int locationY = dateY + metrics.getHeight() + 20;  // Space below date

        // Draw text
        g2d.drawString(dateText, dateX, dateY);
        g2d.drawString(location, locationX, locationY);

        // Clean up
        g2d.dispose();

        // Convert back to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(newImage, "PNG", baos);
        return baos.toByteArray();
    }

}
