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
import org.springframework.web.multipart.MultipartFile;
import scoula.coin.application.dto.RunnerDistanceDTO;
import scoula.coin.application.dto.RunningRecord;
import scoula.coin.application.entity.RunningRecords;
import scoula.coin.domain.run.Repository.RegularRepository;
import scoula.coin.domain.run.Repository.RunningRecordsRepository;
import scoula.coin.domain.run.Service.ImageTextService;
import scoula.coin.domain.run.Service.RunService;

import java.io.IOException;
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

    @Operation(summary = "러닝 기록 저장",description = "사진에서 추출, 수정된 기록 저장")
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
            //@RequestParam("image") MultipartFile file,
            @RequestParam("dateTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm") LocalDateTime dateTime,
            @RequestParam("location") String location,
            @RequestParam(value = "fontName", defaultValue = "Arial") String fontName,
            @RequestParam(value = "fontSize", defaultValue = "50") int fontSize,
            @RequestParam(value = "fontColor", defaultValue = "#FFFFFF") String fontColor) {

        try {
            Resource resource = new ClassPathResource("static/" + "images/bg.png");
            byte[] imageBytes = Files.readAllBytes(resource.getFile().toPath());
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

        } catch (IOException e) {
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
