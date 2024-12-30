package scoula.coin.domain.trading;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import scoula.coin.application.dto.CandleDTO;
import scoula.coin.application.dto.OrderBookDTO;
import scoula.coin.application.entity.TradingSignalHistory;
import scoula.coin.domain.market.CandleService;
import scoula.coin.domain.order.OrderService;
import scoula.coin.domain.run.Repository.TradingSignalHistoryRepository;
import scoula.coin.domain.strategy.TechnicalIndicator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class TradingService {
    private final CandleService candleService;
    private final TechnicalIndicator technicalIndicator;
    private final OrderService orderService;
    private final TradingSignalHistoryRepository signalHistoryRepository;

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

            // 모든 분봉에 대한 신호 생성
            List<Integer> signals = technicalIndicator.generateSignals(
                    prices,
                    rsi,
                    trimmedBollingerBands
            );

            boolean orderExecuted = false;
            String orderStatus = "No signal generated";

            // 미체결 주문 확인 및 취소
            JsonNode waitOrders = orderService.getOrders(market, null, 1, 10, "wait");
            if (waitOrders.has("data") && waitOrders.get("data").isArray()) {
                for (JsonNode order : waitOrders.get("data")) {
                    String uuid = order.get("uuid").asText();

                    // UUID로 시그널 이력 조회
                    TradingSignalHistory signalHistory = signalHistoryRepository.findByOrderUuid(uuid)
                            .orElse(null);

                    if (signalHistory != null) {
                        LocalDateTime orderTime = signalHistory.getCreatedAt();
                        LocalDateTime now = LocalDateTime.now();

                        // 주문 생성 후 3분이 지났는지 확인
                        if (ChronoUnit.MINUTES.between(orderTime, now) >= 3) {
                            log.info("Canceling unfilled order after 3 minutes - UUID: {}, Created At: {}",
                                    uuid, orderTime);

                            try {
                                JsonNode cancelResult = orderService.cancelOrder(uuid);
                                log.info("Order canceled successfully - UUID: {}", uuid);
                                orderStatus = "Canceled unfilled order after 3 minutes: " + uuid;

                                // 시그널 이력 업데이트
                                signalHistory.setFailureReason("Order canceled after 3 minutes timeout");
                                signalHistoryRepository.save(signalHistory);
                            } catch (Exception e) {
                                log.error("Failed to cancel order: {}", e.getMessage());
                            }
                        } else {
                            log.debug("Order {} is still within 3-minute window. Created at: {}",
                                    uuid, orderTime);
                        }
                    }
                }
            }

            // 가격 모니터링 및 매도 주문 확인
            double currentPrice = prices.get(prices.size() - 1);
            JsonNode orders = orderService.getOrders(market, null, 1, 10,"done");

            if (orders.has("data") && orders.get("data").isArray()) {
                for (JsonNode order : orders.get("data")) {
                    if ("bid".equals(order.get("side").asText()) && "done".equals(order.get("state").asText())) {
                        double orderPrice = order.get("price").asDouble();
                        double priceChange = (currentPrice - orderPrice) / orderPrice;

                        if (Math.abs(priceChange) >= 0.05) {
                            log.info("Price change detected - Order Price: {}, Current Price: {}, Change: {}%",
                                    orderPrice, currentPrice, priceChange * 100);

                            BigDecimal btcBalance = orderChance.getAskAccount().getBalance();
                            if (btcBalance.compareTo(BigDecimal.ZERO) > 0) {
                                try {
                                    executeSellOrder(market, currentPrice, orderChance);
                                    orderStatus = String.format("Take profit/Stop loss executed. Price change: %.2f%%", priceChange * 100);
                                    log.info("Take profit/Stop loss order executed at price: {}", currentPrice);
                                    orderExecuted = true;
                                } catch (Exception e) {
                                    log.error("Failed to execute take profit/stop loss order: {}", e.getMessage());
                                }
                            }
                        }
                    }
                }
            }

            // 마지막 분봉의 신호에 대해서만 주문 실행
            if (!signals.isEmpty() && !prices.isEmpty()) {
                int latestSignal = signals.get(signals.size() - 1);
                double lastRsi = rsi.get(rsi.size() - 1);

                if (latestSignal != 0) {
                    log.info("Trading signal detected in last candle - Type: {}, RSI: {}, Price: {}",
                            latestSignal > 0 ? "BUY" : "SELL",
                            lastRsi,
                            currentPrice);

                    try {
                        executeOrder(market, latestSignal, currentPrice, orderChance);
                        orderExecuted = true;
                        orderStatus = "Order executed successfully for last candle signal";
                    } catch (Exception e) {
                        orderStatus = "Order execution failed: " + e.getMessage();
                        log.error("Order execution failed for last candle", e);
                    }
                } else {
                    log.debug("No trading signal in last candle. RSI: {}, Price: {}",
                            lastRsi, currentPrice);
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

    private void executeOrder(String market, int signal, double currentPrice, OrderBookDTO orderChance) {
        TradingSignalHistory signalHistory = TradingSignalHistory.builder()
                .market(market)
                .price(BigDecimal.valueOf(currentPrice))
                .signalType(signal)
                .orderExecuted(false)  // 초기값
                .build();
        signalHistory = signalHistoryRepository.save(signalHistory);

        try {
            if (signal > 0) {  // Buy signal
                executeBuyOrder(market, currentPrice, orderChance);
            } else {  // Sell signal
                executeSellOrder(market, currentPrice, orderChance);
            }
            signalHistory.setOrderExecuted(true);
            signalHistoryRepository.save(signalHistory);
        } catch (Exception e) {
            log.error("Error executing order: " + e.getMessage(), e);
            signalHistory.setFailureReason(e.getMessage());
            signalHistoryRepository.save(signalHistory);
            throw new RuntimeException("Failed to execute order", e);
        }
    }

    private void executeBuyOrder(String market, double currentPrice, OrderBookDTO orderChance) throws Exception {
        // Get KRW balance for buying
        BigDecimal availableBalance = orderChance.getBidAccount().getBalance();
        BigDecimal minOrderSize = BigDecimal.valueOf(5000.0);

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

            executeOrderRequest(market, "bid", volume.doubleValue(), currentPrice);
        } else {
            log.warn("Insufficient KRW balance for buy order. Available: {} KRW", availableBalance);
        }
    }

    private void executeSellOrder(String market, double currentPrice, OrderBookDTO orderChance) throws Exception {
        // Get BTC balance
        BigDecimal btcBalance = orderChance.getAskAccount().getBalance();

        // Minimum BTC order size (0.0001 BTC)
        BigDecimal MIN_BTC_ORDER = new BigDecimal("0.0001");

        // Skip if no BTC balance or balance less than minimum
        if (btcBalance.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("No BTC balance available for sell order");
            return;
        }

        if (btcBalance.compareTo(MIN_BTC_ORDER) < 0) {
            log.warn("BTC balance ({}) is less than minimum order size ({})",
                    btcBalance, MIN_BTC_ORDER);
            return;
        }

        // Calculate 10% of BTC balance
        BigDecimal tenPercentBTC = btcBalance.multiply(BigDecimal.valueOf(0.1))
                .setScale(8, RoundingMode.DOWN);

        // Use larger of 10% and minimum BTC order
        BigDecimal sellVolume = tenPercentBTC.max(MIN_BTC_ORDER);

        // Ensure we don't sell more than available
        if (sellVolume.compareTo(btcBalance) > 0) {
            sellVolume = btcBalance;
        }

        // Log trade details
        BigDecimal totalValueKRW = sellVolume.multiply(BigDecimal.valueOf(currentPrice));
        log.info("Executing sell order - Volume: {} BTC, Price: {} KRW, Total Value: {} KRW",
                sellVolume, currentPrice, totalValueKRW);

        // Execute sell order
        executeOrderRequest(market, "ask", sellVolume.doubleValue(), currentPrice);
    }

    private void executeOrderRequest(String market, String side, double volume, double price) throws Exception {
        Object orderResult = orderService.doOrder(market, side, volume, price, "limit");
        // orderResult에서 UUID 추출하여 시그널 이력 업데이트
        if (orderResult instanceof JsonNode) {
            String uuid = ((JsonNode) orderResult).get("uuid").asText();
            // 가장 최근 시그널 이력 찾아서 업데이트
            TradingSignalHistory latestSignal = signalHistoryRepository
                    .findByMarketOrderByCreatedAtDesc(market)
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (latestSignal != null) {
                latestSignal.setOrderUuid(uuid);
                signalHistoryRepository.save(latestSignal);
            }
        }
    }
}
