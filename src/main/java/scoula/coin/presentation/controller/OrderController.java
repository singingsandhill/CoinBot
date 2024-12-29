package scoula.coin.presentation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import scoula.coin.application.dto.ApiResponse;
import scoula.coin.application.dto.TradingSignalHistoryDTO;
import scoula.coin.application.entity.OrderHistory;
import scoula.coin.application.entity.TradingSignalHistory;
import scoula.coin.domain.order.OrderService;
import scoula.coin.application.dto.OrderHistoryDTO;
import scoula.coin.domain.run.Repository.TradingSignalHistoryRepository;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/coin/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final TradingSignalHistoryRepository tradingSignalHistoryRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderHistory>> createOrder(
            @RequestParam String market,
            @RequestParam String side,
            @RequestParam double volume,
            @RequestParam double price,
            @RequestParam(defaultValue = "limit") String ordType
    ) {
        log.info("Order request - market: {}, side: {}, volume: {}, price: {}, ordType: {}",
                market, side, volume, price, ordType);

        OrderHistory result = null;
        try {
            result = (OrderHistory) orderService.doOrder(market, side, volume, price, ordType);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<OrderHistoryDTO>>> getOrderHistory(
            @RequestParam(required = false) String market) {
        List<OrderHistoryDTO> history = orderService.getOrderHistory(market);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/signals")
    public ResponseEntity<ApiResponse<List<TradingSignalHistoryDTO>>> getSignalHistory(
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

        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getOrders(
            @RequestParam(defaultValue = "KRW-BTC") String market,
            @RequestParam(required = false) List<String> uuids,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "100") Integer limit
    ) {
        Object orders = orderService.getOrders(market, uuids, page, limit);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
}
