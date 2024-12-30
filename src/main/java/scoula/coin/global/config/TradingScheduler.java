package scoula.coin.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import scoula.coin.domain.trading.TradingService;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradingScheduler {
    private final TradingService tradingService;

    //  1분마다 실행되는 스케줄러 (60,000ms)
    @Scheduled(fixedRate = 60_000)
    public void scheduledAnalyzeTradingSignals() {
        try {
            tradingService.analyzeTradingSignals("KRW-BTC", 100);
            log.info("scheduledAnalyzeTradingSignals executed at every 1 minute.");
        } catch (Exception e) {
            log.error("Error in scheduled analyze: ", e);
        }
    }
}
