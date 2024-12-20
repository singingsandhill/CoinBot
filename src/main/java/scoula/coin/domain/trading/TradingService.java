package scoula.coin.domain.trading;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import scoula.coin.application.dto.CandleDTO;
import scoula.coin.domain.market.CandleService;
import scoula.coin.domain.strategy.TechnicalIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class TradingService {
    private final CandleService candleService;
    private final TechnicalIndicator technicalIndicator;

    public Map<String, Object> analyzeTradingSignals(String market, int count) {
        try {
            // 충분한 데이터를 위해 요청 개수를 늘림
            int extendedCount = count + 35; // RSI와 볼린저 밴드 계산을 위한 추가 데이터
            List<CandleDTO> candles = candleService.getCandle(market, extendedCount);

            // 가격 데이터 추출 (최신 데이터가 마지막에 오도록 정렬)
            List<Double> prices = extractPrices(candles);

            // 최소 필요 데이터 확인
            if (prices.size() < 20) { // 볼린저 밴드 기간
                throw new IllegalStateException("Not enough data points for analysis");
            }

            // RSI와 볼린저 밴드 계산
            List<Double> macd = technicalIndicator.calculateMACD(prices, 12, 26, 9);
            List<Double> rsi = technicalIndicator.calculateRSI(prices, 14);
            List<List<Double>> bollingerBands = technicalIndicator.calculateBollingerBands(prices, 20, 2.0);


            // 가장 최근 데이터만 반환 (요청한 count만큼)
            int resultSize = Math.min(count, prices.size());

            // 최신 데이터를 유지하기 위해 끝에서부터 자름
            prices = prices.subList(prices.size() - resultSize, prices.size());
            macd = macd.subList(Math.max(0, macd.size() - resultSize), macd.size());
            rsi = rsi.subList(Math.max(0, rsi.size() - resultSize), rsi.size());

            List<List<Double>> trimmedBollingerBands = new ArrayList<>();
            for (List<Double> band : bollingerBands) {
                trimmedBollingerBands.add(band.subList(Math.max(0, band.size() - resultSize), band.size()));
            }

            // 매매 신호 생성 (RSI와 볼린저 밴드만 사용)
            List<Integer> signals = technicalIndicator.generateSignals(
                    prices,
                    rsi,
                    trimmedBollingerBands
            );

            // 분석 결과 반환
            return Map.of(
                    "prices", prices,
                    "macd", macd,
                    "rsi", rsi,
                    "bollingerBands", trimmedBollingerBands,
                    "signals", signals
            );
        } catch (Exception e) {
            log.error("Error in trading analysis: ", e);
            throw new RuntimeException("Failed to analyze trading signals", e);
        }
    }

    private List<Double> extractPrices(List<CandleDTO> candles) {
        // 최신 데이터가 마지막에 오도록 정렬된 가격 리스트 반환
        List<Double> prices = new ArrayList<>();
        // candles는 이미 시간 역순으로 정렬되어 있으므로, 역순으로 추가하여 시간순 정렬
        for (int i = candles.size() - 1; i >= 0; i--) {
            prices.add(candles.get(i).getTradePrice());
        }
        return prices;
    }
}
