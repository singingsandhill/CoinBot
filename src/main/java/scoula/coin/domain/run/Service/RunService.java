package scoula.coin.domain.run.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import scoula.coin.application.dto.RunningRecord;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RunService {
    private RunningRecord parseRunningRecord(String text) {
        // 줄 단위로 분리
        String[] lines = text.split("\n");

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

    private String extractName(String text) {

        Pattern pattern = Pattern.compile("[가-힣]+\\s*[가-힣]+\\s*[가-힣]?");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String found = matcher.group().trim();
            // 불필요한 매칭 제외
            if (found.equals("방이 을") || found.equals("Progress Activities")) {
                continue;
            }// 공백 제거 및 성씨 처리
            String[] nameParts = found.split("\\s+");
            if (nameParts.length == 3) {
                String firstName = nameParts[0] + nameParts[1];  // 이름 첫 부분
                String lastName = nameParts[2];   // 성씨

                // 성씨 목록과 비교하여 순서 조정
                if (isLastName(lastName)) {
                    return lastName + firstName;  // 성씨가 뒤에 있었다면 앞으로 이동
                } else if (isLastName(firstName)) {
                    return firstName + lastName;  // 이미 성씨가 앞에 있다면 그대로 유지
                }
            }
        }

        return "이름 추출 실패";
    }

    private boolean isLastName(String name) {
        // 한국의 흔한 성씨 목록
        String[] lastNames = {"김", "이", "박", "최", "정", "강", "조", "윤", "장", "임",
                "한", "신", "서", "권", "황", "안", "송", "류", "전", "홍"};

        for (String lastName : lastNames) {
            if (name.equals(lastName)) {
                return true;
            }
        }
        return false;
    }

    private String extractLocation(String text) {
        // Jayang 4(sa)-dong, Seoul 패턴 찾기
        Pattern pattern = Pattern.compile("([A-Za-z]+)\\s+\\d+\\([a-z]+\\)-dong,\\s*Seoul");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }

        // 백업 패턴: 부분적으로라도 주소 찾기
        Pattern backupPattern = Pattern.compile("(Jayang|[A-Za-z]+)\\s*\\d*(?:\\([a-z]+\\))?-dong,\\s*Seoul");
        Matcher backupMatcher = backupPattern.matcher(text);
        if (backupMatcher.find()) {
            return backupMatcher.group();
        }

        // 더 유연한 패턴: 하이픈이 없는 경우도 처리
        Pattern flexPattern = Pattern.compile("(Jayang|[A-Za-z]+)\\s*\\d*(?:\\([a-z]+\\))?\\s*dong,\\s*Seoul");
        Matcher flexMatcher = flexPattern.matcher(text);
        if (flexMatcher.find()) {
            return flexMatcher.group();
        }

        return null;
    }

    private LocalDateTime extractDateTime(String text) {
        // "December 11,2024 at 7:43 PM" 패턴 찾기
        Pattern pattern = Pattern.compile("([A-Za-z]+ \\d{1,2},\\d{4} at \\d{1,2}:\\d{2} [AP]M)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String dateStr = matcher.group(1);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d,yyyy 'at' h:mm a", Locale.ENGLISH);
            return LocalDateTime.parse(dateStr, formatter);
        }
        Pattern koreanPattern = Pattern.compile("(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일[^\\n]*\\n[^0-9]*([0-9]{1,2}):([0-9]{1,2})");
        Matcher koreanMatcher = koreanPattern.matcher(text);
        if (koreanMatcher.find()) {
            int month = Integer.parseInt(koreanMatcher.group(1));
            int day = Integer.parseInt(koreanMatcher.group(2));
            int hour = Integer.parseInt(koreanMatcher.group(3));
            int minute = Integer.parseInt(koreanMatcher.group(4));

            // 현재 연도 사용
            int year = LocalDateTime.now().getYear();

            // 오후인 경우 시간에 12 추가
            if (text.contains("오 후") && hour != 12) {
                hour += 12;
            }

            return LocalDateTime.of(year, month, day, hour, minute);
        }
        return null;
    }

    private double extractDistance(String text) {
        // "Distance" 다음에 나오는 숫자 찾기
        Pattern pattern = Pattern.compile("(\\d+\\.\\d+)\\s*km");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 0.0;
    }

    public String extractPace(String text) {
        // "Pace" 다음에 나오는 시간 형식 찾기
        Pattern pattern = Pattern.compile("(\\d{1,2}:\\d{2}\\s*/km)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        Pattern pattern2 = Pattern.compile("(\\d{1,2}'\\d{1,2}\")");
        Matcher matcher2 = pattern2.matcher(text);
        if (matcher2.find()) {
            String paceStr = matcher2.group(1);
            // 형식 변환 ('를 :로 변환하고 "를 제거)
            paceStr = paceStr.replace("'", ":").replace("\"", "");
            return paceStr + " /km";
        }
        return null;
    }
}
