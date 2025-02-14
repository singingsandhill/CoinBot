package scoula.coin.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import scoula.coin.application.dto.RunnerDistanceDTO;
import scoula.coin.application.dto.RunningRecord;
import scoula.coin.domain.run.Repository.RegularRepository;
import scoula.coin.domain.run.Repository.RunningRecordsRepository;
import scoula.coin.domain.run.Service.ImageTextService;
import scoula.coin.domain.run.Service.RunService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor

public class RunningController {

    private final RunService runService;
    private final RegularRepository regularRepository;
    private final RunningRecordsRepository repository;

    @GetMapping("/running/home")
    public String Home(Model model) {
        return "running/home";
    }

    @GetMapping("/running/record")
    public String recordPage(Model model) {
        return "running/record";
    }

    @GetMapping("/running/regular")
    public String Regular(Model model) {
        return "running/regular";
    }

    @PostMapping("/running/save-ocr")
    @ResponseBody
    @CrossOrigin
    public ResponseEntity<RunningRecord> saveOcrResult(@RequestBody Map<String, String> payload) {
        String extractedText = payload.get("text");
        RunningRecord record = runService.parseRunningRecord(extractedText);
        return ResponseEntity.ok(record);
    }

    @Operation(summary = "러닝 기록 저장", description = "사진에서 추출, 수정된 기록 저장")
    @PostMapping("/running/save")
    @ResponseBody
    public ResponseEntity<RunningRecord> saveRecord(@RequestBody RunningRecord record) {
        runService.saveRunningRecord(record);
        return ResponseEntity.ok(record);
    }

    @GetMapping("/running/runner-distances")
    @ResponseBody
    public List<RunnerDistanceDTO> getRunnerDistances() {
        return runService.getRunnerDistances();
    }

    @GetMapping("/running/mostattempt")
    @ResponseBody
    public List<Object> getMostAttempt() {
        return runService.mostAttempt();
    }

    @GetMapping("/running/attempt")
    @ResponseBody
    public List<Object> getAttempt() {
        return runService.getAttempts();
    }

    private final ImageTextService imageTextService;

    @PostMapping("/running/modify")
    @ResponseBody
    public ResponseEntity<byte[]> modifyImage(
            @RequestParam("dateTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm") LocalDateTime dateTime,
            @RequestParam("location") String location,
            @RequestParam(value = "fontName", defaultValue = "Arial") String fontName,
            @RequestParam(value = "fontSize", defaultValue = "50") int fontSize,
            @RequestParam(value = "fontColor", defaultValue = "#FFFFFF") String fontColor) {

        log.info("이미지 수정 요청 받음 - DateTime: {}, Location: {}, Font: {}", dateTime, location, fontName);

        try {
            Resource resource = new ClassPathResource("static/images/bg.png");
            log.info("리소스 경로: {}", resource.getURI());

            if (!resource.exists()) {
                log.error("이미지 파일을 찾을 수 없음: {}", resource.getFilename());
                return ResponseEntity.notFound().build();
            }

            log.info("이미지 파일 읽기 시작");
            try (InputStream inputStream = resource.getInputStream()) {
                byte[] imageBytes = inputStream.readAllBytes();

                byte[] modifiedImage = imageTextService.addTextToImage(
                        imageBytes,
                        dateTime,
                        location,
                        fontName,
                        fontSize,
                        fontColor
                );

                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .body(modifiedImage);
            }

        } catch (IOException e) {
            log.error("이미지 처리 중 IO 예외 발생", e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("이미지 처리 중 예상치 못한 예외 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/running/upload")
    public String showUploadForm() {
        return "running/makeimage"; // upload.html 템플릿을 반환
    }

    @PostMapping("/running/upload/success")
    public String handleUploadSuccess(Model model) {
        model.addAttribute("message", "이미지가 성공적으로 수정되었습니다!");
        return "success";
    }

    // RunningController.java에 추가할 메서드
    @GetMapping("/running/available-fonts")
    @ResponseBody
    public List<String> getAvailableFonts() {
        return imageTextService.getAvailableFonts();
    }
}
