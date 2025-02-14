package scoula.coin.domain.run.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@Slf4j
@Service
public class ImageTextService {
    private static final String IMAGE_FORMAT = "PNG";
    private static final String DATE_FORMAT_PATTERN = "yyyy.MM.dd (HH:mm)";
    private static final int TEXT_SPACING = 20;

    /**
     * 이미지에 날짜와 위치 텍스트를 추가하는 메서드
     */
    public byte[] addTextToImage(byte[] originalImage,
                                 LocalDateTime dateTime,
                                 String location,
                                 String fontName,
                                 int fontSize,
                                 String fontColor) throws IOException {
        log.info("이미지 텍스트 추가 시작 - Font: {}, Size: {}, Color: {}", fontName, fontSize, fontColor);

        try (ByteArrayInputStream bais = new ByteArrayInputStream(originalImage);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // 원본 이미지 로드
            BufferedImage sourceImage = ImageIO.read(bais);
            if (sourceImage == null) {
                throw new IOException("이미지를 로드할 수 없습니다.");
            }

            // 새 이미지 생성 및 텍스트 추가
            BufferedImage resultImage = createImageWithText(
                    sourceImage,
                    dateTime,
                    location,
                    fontName,
                    fontSize,
                    Color.decode(fontColor)
            );

            // 결과 이미지를 바이트 배열로 변환
            ImageIO.write(resultImage, IMAGE_FORMAT, baos);
            log.info("이미지 텍스트 추가 완료");

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("이미지 처리 중 에러 발생", e);
            throw new IOException("이미지 처리 실패", e);
        }
    }

    /**
     * 실제 이미지 생성 및 텍스트 추가를 수행하는 private 메서드
     */
    private BufferedImage createImageWithText(BufferedImage sourceImage,
                                              LocalDateTime dateTime,
                                              String location,
                                              String fontName,
                                              int fontSize,
                                              Color fontColor) {

        BufferedImage newImage = new BufferedImage(
                sourceImage.getWidth(),
                sourceImage.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = newImage.createGraphics();
        try {
            // 이미지 품질 설정
            configureGraphicsQuality(g2d);

            // 원본 이미지 복사
            g2d.drawImage(sourceImage, 0, 0, null);

            // 폰트 설정
            Font font = new Font(fontName, Font.BOLD, fontSize);
            g2d.setFont(font);
            g2d.setColor(fontColor);

            // 텍스트 위치 계산 및 그리기
            drawTexts(g2d, font, dateTime, location, newImage.getWidth(), newImage.getHeight());

            return newImage;
        } finally {
            g2d.dispose();
        }
    }

    /**
     * Graphics2D 품질 설정을 담당하는 private 메서드
     */
    private void configureGraphicsQuality(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
    }

    /**
     * 텍스트 그리기를 담당하는 private 메서드
     */
    private void drawTexts(Graphics2D g2d,
                           Font font,
                           LocalDateTime dateTime,
                           String location,
                           int width,
                           int height) {
        String dateText = dateTime.format(DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN));
        FontMetrics metrics = g2d.getFontMetrics(font);

        // 날짜 텍스트 위치 계산 및 그리기
        int dateX = (width - metrics.stringWidth(dateText)) / 2;
        int dateY = (height / 2) + 250;
        g2d.drawString(dateText, dateX, dateY);

        // 위치 텍스트 위치 계산 및 그리기
        int locationX = (width - metrics.stringWidth(location)) / 2;
        int locationY = dateY + metrics.getHeight() + TEXT_SPACING;
        g2d.drawString(location, locationX, locationY);
    }

    /**
     * 사용 가능한 글씨체 목록을 반환하는 메서드
     */
    public List<String> getAvailableFonts() {
        return Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getAvailableFontFamilyNames()
        );
    }
}