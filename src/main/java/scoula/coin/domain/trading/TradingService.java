package scoula.coin.domain.trading;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import scoula.coin.application.dto.CandleDTO;
import scoula.coin.application.dto.OrderBookDTO;
import scoula.coin.domain.market.CandleService;
import scoula.coin.domain.order.OrderService;
import scoula.coin.domain.strategy.TechnicalIndicator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class TradingService {
    private final CandleService candleService;
    private final TechnicalIndicator technicalIndicator;
    private final OrderService orderService;

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

            OrderBookDTO orderChance = orderService.getOrderChance(market);

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
            try {
                // 기존 분석 로직 유지
                boolean orderExecuted = false;
                String orderStatus = "No signal generated";

                if (!signals.isEmpty() && !prices.isEmpty()) {
                    int latestSignal = signals.get(signals.size() - 1);
                    double currentPrice = prices.get(prices.size() - 1);

                    if (latestSignal != 0) {
                        try {
                            executeOrder(market, latestSignal, currentPrice, orderChance);
                            orderExecuted = true;
                            orderStatus = "Order executed successfully";
                        } catch (Exception e) {
                            orderStatus = "Order execution failed: " + e.getMessage();
                            log.error("Order execution failed", e);
                        }
                    }
                }

                return Map.of(
                        "prices", prices,
                        "macd", macd,
                        "rsi", rsi,
                        "bollingerBands", trimmedBollingerBands,
                        "signals", signals,
                        "orderExecuted", orderExecuted,
                        "orderStatus", orderStatus
                );
            } catch (Exception e) {
                log.error("Error in trading analysis: ", e);
                throw new RuntimeException("Failed to analyze trading signals", e);
            }
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
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

    /**
     * Executes an order with smart order sizing based on account balance and minimum order requirements.
     * This method ensures orders adhere to exchange constraints while optimizing size for risk management.
     *
     * @param market       The market symbol (e.g., KRW-BTC)
     * @param signal       Trading signal (-1 for sell, 1 for buy)
     * @param currentPrice Current market price
     * @param orderChance  Order chance information with minimum order constraints
     */
    private void executeOrder(String market, int signal, double currentPrice, OrderBookDTO orderChance) {
        try {
            // Get account balance information
            BigDecimal availableBalance;
            BigDecimal minOrderSize;

            if (signal > 0) {  // Buy signal
                // Get KRW balance for buying
                availableBalance = orderChance.getBidAccount().getBalance();
                minOrderSize = orderChance.getMarket().getBid().getMinTotal();
            } else {  // Sell signal
                // Get crypto balance for selling
                availableBalance = orderChance.getAskAccount().getBalance();
                minOrderSize = orderChance.getMarket().getAsk().getMinTotal();
            }

            // Calculate order size (10% of available balance)
            BigDecimal orderSize = availableBalance.multiply(BigDecimal.valueOf(0.1))
                    .setScale(8, RoundingMode.DOWN);

            // Ensure order size is at least the minimum required
            if (orderSize.compareTo(minOrderSize) < 0) {
                orderSize = minOrderSize;
            }

            // Check if we have sufficient balance
            if (availableBalance.compareTo(orderSize) >= 0) {
                // Calculate volume based on current price
                BigDecimal volume = orderSize.divide(
                        BigDecimal.valueOf(currentPrice),
                        8,
                        RoundingMode.DOWN
                );

                // Execute order
                orderService.doOrder(market,
                        signal > 0 ? "bid" : "ask",
                        volume.doubleValue(),
                        currentPrice,
                        "limit"
                );

                log.info("Order executed: {} {} {} at price {}",
                        signal > 0 ? "Buy" : "Sell",
                        volume,
                        market,
                        currentPrice
                );
            } else {
                log.warn("Insufficient balance for order execution");
            }
        } catch (Exception e) {
            log.error("Error executing order: " + e.getMessage(), e);
            throw new RuntimeException("Failed to execute order", e);
        }
    }
}
