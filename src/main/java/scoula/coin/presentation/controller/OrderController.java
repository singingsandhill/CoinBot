package scoula.coin.presentation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import scoula.coin.application.dto.TradingSignalHistoryDTO;
import scoula.coin.application.entity.TradingSignalHistory;
import scoula.coin.domain.order.OrderService;
import scoula.coin.application.dto.OrderHistoryDTO;
import scoula.coin.domain.run.Repository.TradingSignalHistoryRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/coin")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final TradingSignalHistoryRepository tradingSignalHistoryRepository;

    @PostMapping("/order")
    public ResponseEntity<?> createOrder(
            @RequestParam String market,
            @RequestParam String side,
            @RequestParam double volume,
            @RequestParam double price,
            @RequestParam(defaultValue = "limit") String ordType
    ) {
        try {
            // 로깅 추가
            log.info("Order request - market: {}, side: {}, volume: {}, price: {}, ordType: {}",
                    market, side, volume, price, ordType);

            Object result = orderService.doOrder(market, side, volume, price, ordType);

            // 성공 로깅
            log.info("Order created successfully: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // 상세 에러 로깅
            log.error("Order creation failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error", "Order creation failed",
                            "message", e.getMessage()
                    )
            );
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<OrderHistoryDTO>> getOrderHistory(
            @RequestParam(required = false) String market) {
        List<OrderHistoryDTO> history = orderService.getOrderHistory(market);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/signals")
    public ResponseEntity<List<TradingSignalHistoryDTO>> getSignalHistory(
            @RequestParam(required = false) String market,
            @RequestParam(required = false) Integer signalType,
            @RequestParam(required = false) Boolean executed) {

        List<TradingSignalHistory> signals;
        if (market != null) {
            signals = tradingSignalHistoryRepository.findByMarketOrderByCreatedAtDesc(market);
        } else if (signalType != null) {
            signals = tradingSignalHistoryRepository.findBySignalTypeOrderByCreatedAtDesc(signalType);
        } else if (executed != null) {
            signals = tradingSignalHistoryRepository.findByOrderExecutedOrderByCreatedAtDesc(executed);
        } else {
            signals = (List<TradingSignalHistory>) tradingSignalHistoryRepository.findAll();
        }

        List<TradingSignalHistoryDTO> dtos = signals.stream()
                .map(TradingSignalHistoryDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
}
