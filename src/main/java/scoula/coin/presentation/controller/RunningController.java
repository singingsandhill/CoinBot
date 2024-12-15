package scoula.coin.presentation.controller;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import scoula.coin.application.dto.RunnerDistanceDTO;
import scoula.coin.application.dto.RunningRecord;
import scoula.coin.application.entity.RunningRecords;
import scoula.coin.domain.run.Repository.RegularRepository;
import scoula.coin.domain.run.Repository.RunningRecordsRepository;
import scoula.coin.domain.run.Service.RunService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Controller
@RequiredArgsConstructor

public class RunningController {

    private final RunService runService;
    private final RegularRepository regularRepository;

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
        try {
            RunningRecord record = runService.parseRunningRecord(extractedText);
            log.info("Parsed record: {}", record);
            // TODO: 저장 로직 구현
            return ResponseEntity.ok(record);  // 여기서 파싱된 record 객체를 반환
        } catch (Exception e) {
            log.error("Failed to parse running record", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/running/save")
    @ResponseBody
    public ResponseEntity<RunningRecord> saveRecord(@RequestBody RunningRecord record) {
        try {
            // Convert RunningRecord to RunningRecords entity
            RunningRecords entity = RunningRecords.builder()
                    .name(record.getName())
                    .location(record.getLocation())
                    .dateTime(record.getDateTime())
                    .distance(record.getDistance())
                    .pace(record.getPace())
                    .build();

            // Save to database
            RunningRecords savedRecord = repository.save(entity);
            log.info("Saved record to database: {}", savedRecord);

            return ResponseEntity.ok(record);
        } catch (Exception e) {
            log.error("Failed to save running record", e);
            return ResponseEntity.badRequest().build();
        }
    }
    private final RunningRecordsRepository repository;

    @GetMapping("/running/runner-distances")
    @ResponseBody
    public List<RunnerDistanceDTO> getRunnerDistances() {
        return repository.findTotalDistanceByRunner();
    }

    @GetMapping("/running/mostattempt")
    @ResponseBody
    public List<Object> getMostAttempt() {
        return runService.mostattempt();
    }

    @GetMapping("/running/attempt")
    @ResponseBody
    public List<Object> getAttempt() {
        return regularRepository.findAllof();
    }
}
