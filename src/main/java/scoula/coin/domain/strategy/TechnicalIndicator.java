package scoula.coin.domain.strategy;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
/**
 * 지표 계산하는 서비스 계층
 */
public class TechnicalIndicator {

    /**
     * 장단기 이동평균선간의 차이 계산
     * @param prices : List<Double>
     * @param fastPeriod : int
     * @param slowPeriod : int
     * @param signalPeriod : int
     * @return : List<Double>
     */
    public List<Double> calculateMACD(List<Double> prices, int fastPeriod, int slowPeriod, int signalPeriod) {
        if (prices.size() < slowPeriod) {
            return new ArrayList<>();
        }

        List<Double> fastEMA = calculateEMA(prices, fastPeriod);
        List<Double> slowEMA = calculateEMA(prices, slowPeriod);

        // MACD 라인 계산
        List<Double> macdLine = new ArrayList<>();
        for (int i = 0; i < fastEMA.size() && i < slowEMA.size(); i++) {
            macdLine.add(fastEMA.get(i) - slowEMA.get(i));
        }

        // 시그널 라인 계산
        return calculateEMA(macdLine, signalPeriod);
    }

    public List<Double> calculateEMA(List<Double> prices, int period) {
        if (prices.size() < period) {
            return new ArrayList<>();
        }

        List<Double> ema = new ArrayList<>();
        double multiplier = 2.0 / (period + 1);

        // Initial SMA calculation
        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += prices.get(i);
        }
        double sma = sum / period;
        ema.add(sma);

        // EMA calculation
        for (int i = period; i < prices.size(); i++) {
            double newEMA = (prices.get(i) - ema.get(ema.size() - 1)) * multiplier + ema.get(ema.size() - 1);
            ema.add(newEMA);
        }

        return ema;
    }

    /**
     * 상대강도지수 계산
     * @param prices : List<Double>
     * @param period : int
     * @return : List<Double>
     */
    public List<Double> calculateRSI(List<Double> prices, int period) {
        if (prices.size() < period + 1) {
            return new ArrayList<>();
        }

        List<Double> rsi = new ArrayList<>();
        List<Double> gains = new ArrayList<>();
        List<Double> losses = new ArrayList<>();

        // Calculate price changes
        for (int i = 1; i < prices.size(); i++) {
            double change = prices.get(i) - prices.get(i - 1);
            gains.add(Math.max(change, 0));
            losses.add(Math.max(-change, 0));
        }

        // Initial averages
        double avgGain = 0;
        double avgLoss = 0;
        for (int i = 0; i < period; i++) {
            avgGain += gains.get(i);
            avgLoss += losses.get(i);
        }
        avgGain /= period;
        avgLoss /= period;

        // First RSI
        double rs = avgGain / Math.max(avgLoss, 0.0001); // Avoid division by zero
        rsi.add(100 - (100 / (1 + rs)));

        // Calculate subsequent RSI values
        for (int i = period; i < prices.size() - 1; i++) {
            avgGain = ((avgGain * (period - 1)) + gains.get(i)) / period;
            avgLoss = ((avgLoss * (period - 1)) + losses.get(i)) / period;
            rs = avgGain / Math.max(avgLoss, 0.0001);
            rsi.add(100 - (100 / (1 + rs)));
        }

        return rsi;
    }

    /**
     * 볼린저 밴드 계산
     * @param prices : List<Double>
     * @param period : int
     * @param stdDev : double
     * @return : List<List<Double>>
     */
    public List<List<Double>> calculateBollingerBands(List<Double> prices, int period, double stdDev) {
        if (prices.size() < period) {
            return List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        List<Double> sma = new ArrayList<>();
        List<Double> upperBand = new ArrayList<>();
        List<Double> lowerBand = new ArrayList<>();

        for (int i = period - 1; i < prices.size(); i++) {
            List<Double> window = prices.subList(i - period + 1, i + 1);
            double avg = window.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double variance = window.stream()
                    .mapToDouble(x -> Math.pow(x - avg, 2))
                    .average()
                    .orElse(0);
            double standardDeviation = Math.sqrt(variance);

            sma.add(avg);
            upperBand.add(avg + (standardDeviation * stdDev));
            lowerBand.add(avg - (standardDeviation * stdDev));
        }

        return List.of(upperBand, sma, lowerBand);
    }

    /**
     * 거래 신호 생성
     * @param prices
     * @param rsi
     * @param macd
     * @param bollingerBands
     * @return
     */
    public List<Integer> generateSignals(List<Double> prices, List<Double> rsi, List<Double> macd, List<List<Double>> bollingerBands) {
        List<Integer> signals = new ArrayList<>();
        signals.add(0); // 첫 포인트는 신호 없음

        for (int i = 1; i < prices.size(); i++) {
            int signal = 0;
            double price = prices.get(i);
            double lastRsi = i < rsi.size() ? rsi.get(i) : 0.0;
            double macdValue = macd.size() > i ? macd.get(i) : 0.0;

            // 직전 MACD (i-1) 값
            double prevMacd = (i - 1) < macd.size() ? macd.get(i - 1) : 0.0;

            // 볼린저 밴드 위치 계산
            double pricePosition = calculatePricePosition(prices.get(i),
                    bollingerBands.get(0).get(i), // upper
                    bollingerBands.get(2).get(i)  // lower
            );

            // 매수 신호 조건
            if (lastRsi <= 30
                    && pricePosition < 0.3
                    && prevMacd < 0
                    && macdValue > 0) {
                signal = 1;  // BUY
            }

            // 매도 신호 조건
            if (lastRsi >= 70
                    && pricePosition > 0.7
                    && prevMacd > 0
                    && macdValue < 0) {
                signal = -1; // SELL
            }

            signals.add(signal);
        }

        return signals;
    }

    // 피봇 저점 확인
    private boolean isPivotBottom(List<Double> prices, int currentIndex, int window) {
        if (currentIndex < window || currentIndex >= prices.size() - window) {
            return false;
        }

        double currentPrice = prices.get(currentIndex);
        boolean isBottom = true;

        // 이전 구간 체크
        for (int i = currentIndex - window; i < currentIndex; i++) {
            if (prices.get(i) < currentPrice) {
                isBottom = false;
                break;
            }
        }

        // 이후 구간 체크
        for (int i = currentIndex + 1; i <= currentIndex + window; i++) {
            if (prices.get(i) < currentPrice) {
                isBottom = false;
                break;
            }
        }

        return isBottom;
    }

    // 피봇 고점 확인
    private boolean isPivotTop(List<Double> prices, int currentIndex, int window) {
        if (currentIndex < window || currentIndex >= prices.size() - window) {
            return false;
        }

        double currentPrice = prices.get(currentIndex);
        boolean isTop = true;

        // 이전 구간 체크
        for (int i = currentIndex - window; i < currentIndex; i++) {
            if (prices.get(i) > currentPrice) {
                isTop = false;
                break;
            }
        }

        // 이후 구간 체크
        for (int i = currentIndex + 1; i <= currentIndex + window; i++) {
            if (prices.get(i) > currentPrice) {
                isTop = false;
                break;
            }
        }

        return isTop;
    }

    // 모멘텀 계산
    private double calculateMomentum(List<Double> prices, int currentIndex, int period) {
        if (currentIndex < period) {
            return 0;
        }
        return prices.get(currentIndex) - prices.get(currentIndex - period);
    }

    /**
     * 볼린저 밴드 내에서의 가격 위치 계산 (0~1 사이 값)
     * @param price : double
     * @param upper : double
     * @param lower : double
     * @return : double
     */
    private double calculatePricePosition(double price, double upper, double lower) {
        return (price - lower) / (upper - lower);
    }

    /**
     * TODO :
     *  1. 분봉 정보 DB화
     *  2. order history 차트에 그리기
     */
}
