package scoula.coin.presentation.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import scoula.coin.domain.market.CandleService;
import scoula.coin.domain.trading.TradingService;

import java.util.Map;

@Log4j2
@Controller
@RequestMapping("/market")
@RequiredArgsConstructor
public class ChartController {

    private final CandleService candleService;
    private final TradingService tradingService;
    private final ObjectMapper objectMapper;

    @GetMapping("/chart")
    public String getMarketChart(
            @RequestParam(defaultValue = "KRW-BTC") String market,
            @RequestParam(defaultValue = "100") int count,
            Model model) {
        try {
            Map<String, Object> analysis = tradingService.analyzeTradingSignals(market, count);
            //log.error("Analysis data: " + analysis);
            String analysisJson = objectMapper.writeValueAsString(analysis);
            //log.error("JSON data: " + analysisJson);

            model.addAttribute("market", market);
            model.addAttribute("count", count);
            model.addAttribute("analysis", analysisJson);

            return "market/chart";
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            // 예외 처리
            model.addAttribute("error", "Failed to process market data");
            return "error";
        }
    }

    @ResponseBody
    @GetMapping("/data")
    public Map<String, Object> getMarketData(
            @RequestParam(defaultValue = "KRW-BTC") String market,
            @RequestParam(defaultValue = "100") int count) {
        return tradingService.analyzeTradingSignals(market, count);
    }
}
