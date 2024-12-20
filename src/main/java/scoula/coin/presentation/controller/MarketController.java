package scoula.coin.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import scoula.coin.application.dto.CandleDTO;
import scoula.coin.domain.market.CandleService;
import scoula.coin.domain.order.OrderService;
import scoula.coin.domain.trading.TradingService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/market")
@RequiredArgsConstructor
public class MarketController {

    private final CandleService candleService;
    private final TradingService tradingService;
    private final OrderService orderService;

    @GetMapping("/candle")
    public ResponseEntity<?> getCandle(@RequestParam(defaultValue = "KRW-BTC") String market,
                                       @RequestParam(defaultValue = "10") int count) {
        List<CandleDTO> candles = candleService.getCandle(market, count);
        return ResponseEntity.ok(candles);
    }

    @ResponseBody
    @GetMapping("/data")
    public Map<String, Object> getMarketData(
            @RequestParam(defaultValue = "KRW-BTC") String market,
            @RequestParam(defaultValue = "100") int count) {
        return tradingService.analyzeTradingSignals(market, count);
    }

    @GetMapping("/analysis")
    public ResponseEntity<?> getMarketAnalysis(
            @RequestParam(defaultValue = "KRW-BTC") String market,
            @RequestParam(defaultValue = "100") int count) {
        return ResponseEntity.ok(tradingService.analyzeTradingSignals(market, count));
    }

    @GetMapping("/orderchance")
    public ResponseEntity<?> getOrderChance(){
        return ResponseEntity.ok(orderService.getOrderChance("KRW-BTC"));
    }

}
