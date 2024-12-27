package scoula.coin.presentation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import scoula.coin.domain.order.OrderService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/coin/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
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
}
