package scoula.coin.application.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter @Setter
@RequiredArgsConstructor
/**
 * API에서 불러오는 시장 정보
 */
public class CandleDTO {
    private String market;
    private String candleDateTimeUtc;
    private String candleDateTimeKst;
    private Double openingPrice;
    private Double highPrice;
    private Double lowPrice;
    private Double tradePrice;
    private Long timeStamp;
    private Double candleAccTradePrice;
    private Double candleAccTradeVolume;
    private Integer unit;
}
