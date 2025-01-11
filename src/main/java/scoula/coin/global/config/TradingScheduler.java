package scoula.coin.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import scoula.coin.domain.trading.TradingService;

@Slf4j
@Component
@RequiredArgsConstructor
/**
 * 스케쥴러
 */
public class TradingScheduler {
    private final TradingService tradingService;

    @Scheduled(fixedRate = 60_000)
    /**
     * 1분마다 자동 실행 되도록 설정.
     * analyzeTradingSignals 실행
     */
    public void scheduledAnalyzeTradingSignals() {
        try {
            tradingService.analyzeTradingSignals("KRW-BTC", 100);
            log.info("scheduledAnalyzeTradingSignals executed at every 1 minute.");
        } catch (Exception e) {
            log.error("Error in scheduled analyze: ", e);
        }
    }
}
