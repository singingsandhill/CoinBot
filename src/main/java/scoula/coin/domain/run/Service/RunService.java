package scoula.coin.domain.run.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import scoula.coin.application.dto.RunningRecord;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RunService {

    public RunningRecord parseRunningRecord(String text) {
        String name = extractName(text);
        String location = extractLocation(text);
        LocalDateTime datetime = extractDateTime(text);
        double distance = extractDistance(text);
        String pace = extractPace(text);

        return RunningRecord.builder()
                .name(name)
                .location(location)
                .dateTime(datetime)
                .distance(distance)
                .pace(pace)
                .build();
    }

    public String extractName(String text) {
        // 첫 번째 패턴: "거리 이 정 혁"
        Pattern pattern1 = Pattern.compile("거리\\s+([가-힣]\\s+[가-힣]\\s+[가-힣])");
        Matcher matcher1 = pattern1.matcher(text);
        if (matcher1.find()) {
            String name = matcher1.group(1)
                    .replaceAll("\\s+", "");  // 모든 공백 제거
            return name;
        }

        // 두 번째 패턴: "이름 성" 형식 -> "성이름" 으로 변환
        Pattern pattern2 = Pattern.compile("([가-힣])\\s+([가-힣])\\s+([가-힣])");
        Matcher matcher2 = pattern2.matcher(text);
        if (matcher2.find()) {
            return matcher2.group(3) + matcher2.group(1) + matcher2.group(2);
        }

        return "이름 추출 실패";
    }

    public String extractLocation(String text) {
        // "당 산 동 2 가 영 등 포 구" 패턴
        Pattern pattern1 = Pattern.compile("([가-힣]\\s+[가-힣]\\s+동\\s+\\d+\\s+가\\s+[가-힣]\\s+[가-힣]\\s+[가-힣]\\s+구)");
        Matcher matcher1 = pattern1.matcher(text);
        if (matcher1.find()) {
            return matcher1.group(1).replaceAll("\\s+", " ").trim();
        }

        // 기존 서울특별시 패턴
        Pattern pattern2 = Pattern.compile("서\\s*울\\s*특\\s*별\\s*시");
        Matcher matcher2 = pattern2.matcher(text);
        if (matcher2.find()) {
            return "서울특별시";
        }

        // 기존 "-dong, Seoul" 패턴
        Pattern pattern3 = Pattern.compile("([A-Za-z]+)\\s+\\d+(?:\\([a-z]+\\))?-dong,\\s+Seoul");
        Matcher matcher3 = pattern3.matcher(text);
        if (matcher3.find()) {
            return matcher3.group().trim();
        }

        return "위치 추출 실패";
    }

    public String extractPace(String text) {
        // "@)" 다음의 "숫자:숫자 km" 패턴
        Pattern pattern = Pattern.compile("@\\)\\s*(\\d+:\\d+)\\s*km");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1) + " /km";
        }

        // "평균 페이스" 옆의 "숫자:숫자 km" 패턴
        Pattern pattern1 = Pattern.compile("평\\s*균\\s*페\\s*이\\s*스\\s*(\\d+:\\d+)\\s*km");
        Matcher matcher1 = pattern1.matcher(text);
        if (matcher1.find()) {
            return matcher1.group(1) + " /km";
        }

        // 기존 "평균 페이스" 뒤의 숫자'숫자" 패턴
        Pattern pattern2 = Pattern.compile("평\\s*균\\s*페\\s*이\\s*스\\s*(\\d+'\\d+\")");
        Matcher matcher2 = pattern2.matcher(text);
        if (matcher2.find()) {
            return matcher2.group(1) + " /km";
        }

        // 기존 숫자'숫자" 패턴
        Pattern pattern3 = Pattern.compile("(\\d+'\\d+\")");
        Matcher matcher3 = pattern3.matcher(text);
        if (matcher3.find()) {
            return matcher3.group(1) + " /km";
        }

        // 기존 숫자:숫자 /km 패턴
        Pattern pattern4 = Pattern.compile("(\\d{1,2}:\\d{2}\\s*/km)");
        Matcher matcher4 = pattern4.matcher(text);
        if (matcher4.find()) {
            return matcher4.group(1);
        }

        return "페이스 추출 실패";
    }

    public LocalDateTime extractDateTime(String text) {
        // 영어 날짜 형식: "December 11,2024 at 7:43 PM"
        Pattern englishPattern = Pattern.compile("([A-Za-z]+ \\d{1,2},\\d{4} at \\d{1,2}:\\d{2} [AP]M)");
        Matcher englishMatcher = englishPattern.matcher(text);
        if (englishMatcher.find()) {
            String dateStr = englishMatcher.group(1);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d,yyyy 'at' h:mm a", Locale.ENGLISH);
            return LocalDateTime.parse(dateStr, formatter);
        }

        // "10 월 20 일 일 요 일 오 후 8:09" 형식
        Pattern koreanPattern1 = Pattern.compile("(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일.*?오\\s*후\\s*(\\d{1,2}):(\\d{2})");
        Matcher koreanMatcher1 = koreanPattern1.matcher(text);
        if (koreanMatcher1.find()) {
            int month = Integer.parseInt(koreanMatcher1.group(1));
            int day = Integer.parseInt(koreanMatcher1.group(2));
            int hour = Integer.parseInt(koreanMatcher1.group(3));
            int minute = Integer.parseInt(koreanMatcher1.group(4));

            // 오후인 경우 12를 더함 (12시는 제외)
            if (hour != 12) {
                hour += 12;
            }

            return LocalDateTime.of(LocalDateTime.now().getYear(), month, day, hour, minute);
        }

        // 기존 한국어 형식
        Pattern koreanPattern2 = Pattern.compile("(\\d{1,2})월 (\\d{1,2})일 (오전|오후) (\\d{1,2}):(\\d{2})");
        Matcher koreanMatcher2 = koreanPattern2.matcher(text);
        if (koreanMatcher2.find()) {
            int month = Integer.parseInt(koreanMatcher2.group(1));
            int day = Integer.parseInt(koreanMatcher2.group(2));
            String amPm = koreanMatcher2.group(3);
            int hour = Integer.parseInt(koreanMatcher2.group(4));
            int minute = Integer.parseInt(koreanMatcher2.group(5));

            if ("오후".equals(amPm) && hour != 12) {
                hour += 12;
            } else if ("오전".equals(amPm) && hour == 12) {
                hour = 0;
            }

            return LocalDateTime.of(LocalDateTime.now().getYear(), month, day, hour, minute);
        }

        return LocalDateTime.now();  // 날짜 추출 실패시 현재 시간
    }

    public double extractDistance(String text) {
        Pattern pattern = Pattern.compile("진\\s*행\\s*상\\s*황\\s*이\\s*어\\s*떻\\s*습\\s*니\\s*까\\s*\\?[\\s\\n\\r]*(\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }

        // "거리" 아래에 있는 숫자.숫자 패턴
        Pattern pattern1 = Pattern.compile("거리\\s*[\\n\\r]+\\s*(\\d+\\.\\d+)");
        Matcher matcher1 = pattern1.matcher(text);
        if (matcher1.find()) {
            return Double.parseDouble(matcher1.group(1));
        }

        // 기존 "숫자.숫자.. ®" 패턴
        Pattern pattern2 = Pattern.compile("(\\d+\\.\\d+)\\.\\.\\s*®");
        Matcher matcher2 = pattern2.matcher(text);
        if (matcher2.find()) {
            return Double.parseDouble(matcher2.group(1));
        }

        // 기존 km 패턴
        Pattern pattern3 = Pattern.compile("(\\d+\\.\\d+)\\s*km");
        Matcher matcher3 = pattern3.matcher(text);
        if (matcher3.find()) {
            return Double.parseDouble(matcher3.group(1));
        }

        return 0.0;
    }

}
