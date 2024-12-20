package scoula.coin.application.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class Market {
    private String id;
    private String name;
    private List<String> order_types;
    private List<String> ask_types;
    private List<String> bid_types;
    private List<String> order_sides;
    private OrderConstraint bid;
    private OrderConstraint ask;
    private BigDecimal max_total;
    private String state;
}
