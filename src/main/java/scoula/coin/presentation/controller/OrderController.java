package scoula.coin.presentation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import scoula.coin.application.dto.ApiResponse;
import scoula.coin.application.dto.TradingSignalHistoryDTO;
import scoula.coin.application.entity.TradingSignalHistory;
import scoula.coin.domain.order.OrderService;
import scoula.coin.application.dto.OrderHistoryDTO;
import scoula.coin.domain.run.Repository.TradingSignalHistoryRepository;

import java.util.stream.Collectors;
import java.util.List;


@Slf4j
@RestController
@RequestMapping("/coin/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final TradingSignalHistoryRepository tradingSignalHistoryRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createOrder(
            @RequestParam String market,
            @RequestParam String side,
            @RequestParam double volume,
            @RequestParam double price,
            @RequestParam(defaultValue = "limit") String ordType
    ) {
        log.info("Order request - market: {}, side: {}, volume: {}, price: {}, ordType: {}",
                market, side, volume, price, ordType);

        Object result = orderService.doOrder(market, side, volume, price, ordType);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<JsonNode>> cancelOrder(@PathVariable String uuid) {
        log.info("Order cancellation request - UUID: {}", uuid);
        JsonNode result = orderService.cancelOrder(uuid);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<?>> getOrderHistory(
            @RequestParam(required = false) String market) {
        List<OrderHistoryDTO> history = orderService.getOrderHistory(market);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/signals")
    public ResponseEntity<ApiResponse<?>> getSignalHistory(
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
    public ResponseEntity<ApiResponse<?>> getOrders(
            @RequestParam(defaultValue = "KRW-BTC") String market,
            @RequestParam(required = false) List<String> uuids,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "100") Integer limit,
            @RequestParam(required = false, defaultValue = "done") String state
    ) {
        Object orders = orderService.getOrders(market, uuids, page, limit,state);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
}
