package scoula.coin.domain.stock.Service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StockService {

    @Value("${mystock.app_key}")
    private String appKey;
}
