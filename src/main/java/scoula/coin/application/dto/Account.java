package scoula.coin.application.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Account {
    private String currency;
    private BigDecimal balance;
    private BigDecimal locked;
    private BigDecimal avg_buy_price;
    private Boolean avg_buy_price_modified;
    private String unit_currency;
}
