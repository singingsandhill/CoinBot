package scoula.coin.domain.stock.Service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import scoula.coin.application.dto.ApiResponse;

@Service
public class StockService {

    @Value("${mystock.app_key}")
    private String appKey;

    public ApiResponse getValance(String symbol) {
        return null;
    }

    public ApiResponse getStockInfo(String symbol) {
        return null;
    }

    /**
     * JSON 데이터 차트로 그리기
     * @param symbol
     * @return
     */
    public Object Chart(String symbol) {
        return null;
    }
}
