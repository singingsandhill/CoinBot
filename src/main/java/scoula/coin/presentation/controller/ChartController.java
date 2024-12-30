package scoula.coin.presentation.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;
import java.util.Map;

@Log4j2
@Controller
@RequestMapping("/market")
@RequiredArgsConstructor
public class ChartController {

    private final CandleService candleService;
    private final TradingService tradingService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "chart화면", description = "API로 candle차트 불러와서 지표 계산")
    @GetMapping("/chart")
    public String getMarketChart(
            @RequestParam(defaultValue = "KRW-BTC") String market,
            @RequestParam(defaultValue = "100") int count,
            Model model) throws JsonProcessingException {

        Map<String, Object> analysis = tradingService.getLatestAnalysisResult();

        if (analysis == null) {
            // 아직 스케줄링이 돌기 전이라면
            analysis = tradingService.analyzeTradingSignals(market, count);
        }

        String analysisJson = objectMapper.writeValueAsString(analysis);

        model.addAttribute("market", market);
        model.addAttribute("count", count);
        model.addAttribute("analysis", analysisJson);
        model.addAttribute("orderExecuted", analysis.get("orderExecuted"));
        model.addAttribute("orderStatus", analysis.get("orderStatus"));

        return "market/chart";
    }
}
