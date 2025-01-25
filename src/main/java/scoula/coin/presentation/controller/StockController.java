package scoula.coin.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import scoula.coin.domain.stock.Service.StockService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stock")
public class StockController {

    private final StockService stockService;

    @GetMapping("/generatetoken")
    public String generatetoken() {
        return null;
    }

    /**
     * 계좌의 잔고를 조회
     * @return
     */
    @GetMapping("/valance")
    public String getValance() {
        return null;
    }

    /**
     * 특정 종목 정보 불러오기
     * @return
     */
    @GetMapping("/info")
    public String getInfo() {
        return null;
    }
}
