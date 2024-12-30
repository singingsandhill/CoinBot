package scoula.coin.domain.order;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.stereotype.Service;
import scoula.coin.application.dto.OrderBookDTO;
import scoula.coin.application.dto.OrderHistoryDTO;
import scoula.coin.application.entity.OrderHistory;
import scoula.coin.domain.order.Repository.OrderHistoryRepository;
import scoula.coin.global.error.CustomException;
import scoula.coin.global.error.ErrorCode;
import scoula.coin.global.util.OrderUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final String BASE_URL  = "https://api.bithumb.com";

    private final OrderUtils orderUtils;
    private final OrderHistoryRepository orderHistoryRepository;

    /**
     * 주문 가능 조건 조회
     * @param market : String
     * @return : OrderBookDTO
     */
    public OrderBookDTO getOrderChance(String market) {
        try {
            validateMarket(market);

            List<NameValuePair> queryParams = Collections.singletonList(
                    new BasicNameValuePair("market", market)
            );

            JsonNode dataNode = orderUtils.executeGetRequest(
                    BASE_URL,
                    "/v1/orders/chance",
                    queryParams
            );

            return OrderBookDTO.builder()
                    .bidFee(getBigDecimal(dataNode, "bid_fee"))
                    .askFee(getBigDecimal(dataNode, "ask_fee"))
                    .makerBidFee(getBigDecimal(dataNode, "maker_bid_fee"))
                    .makerAskFee(getBigDecimal(dataNode, "maker_ask_fee"))
                    .market(mapMarket(dataNode.get("market")))
                    .bidAccount(mapAccount(dataNode.get("bid_account")))
                    .askAccount(mapAccount(dataNode.get("ask_account")))
                    .build();

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get order chance: {}", e.getMessage());
            throw new CustomException(ErrorCode.ORDER_EXECUTION_FAILED);
        }
    }

    /**
     * 주문 생성
     * @param market : String
     * @param side : String
     * @param volume : double
     * @param price : double
     * @param ordType : String
     * @return : OrderHistory
     */
    public OrderHistory doOrder(String market, String side, double volume, double price, String ordType) {
        try {
            validateOrderParameters(market, side, volume, price, ordType);

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("market", market);
            requestBody.put("side", side);
            requestBody.put("volume", volume);
            requestBody.put("price", price);
            requestBody.put("ord_type", ordType);

            JsonNode responseNode = orderUtils.executePostRequest(
                    BASE_URL,
                    "/v1/orders",
                    requestBody
            );

            OrderHistory orderHistory = buildOrderHistory(responseNode);
            return orderHistoryRepository.save(orderHistory);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to execute order: {}", e.getMessage());
            throw new CustomException(ErrorCode.ORDER_EXECUTION_FAILED);
        }
    }

    /**
     * 주문 조회 (DB)
     * @param market : String
     * @return : List<OrderHistoryDTO>
     */
    public List<OrderHistoryDTO> getOrderHistory(String market) {
        try {
            validateMarket(market);
            return orderHistoryRepository.findByMarketOrderByCreatedAtDesc(market)
                    .stream()
                    .map(OrderHistoryDTO::fromEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get order history: {}", e.getMessage());
            throw new CustomException(ErrorCode.ORDER_NOT_FOUND);
        }
    }

    /**
     * 주문 조회 (API)
     * @param market : String
     * @param uuids : List<String>
     * @param page : Integer
     * @param limit : Integer
     * @return : JsonNode
     */
    public JsonNode getOrders(String market, List<String> uuids, Integer page, Integer limit,String state) {
        try {
            validateMarket(market);
            List<NameValuePair> queryParams = createOrderQueryParams(market, uuids, page, limit,state);

            return orderUtils.executeGetRequest(BASE_URL, "/v1/orders", queryParams);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get orders: {}", e.getMessage());
            throw new CustomException(ErrorCode.ORDER_EXECUTION_FAILED);
        }
    }

    /**
     * 주문 취소
     * @param uuid : String
     * @return : JsonNode
     */
    public JsonNode cancelOrder(String uuid) {
        try {
            validateUuid(uuid);

            List<NameValuePair> queryParams = Collections.singletonList(
                    new BasicNameValuePair("uuid", uuid)
            );

            JsonNode response = orderUtils.executeDeleteRequest(
                    BASE_URL,
                    "/v1/order",
                    queryParams
            );

            log.info("Successfully canceled order with UUID: {}", uuid);
            return response;

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to cancel order: {}", e.getMessage());
            throw new CustomException(ErrorCode.ORDER_DELETION_FAILED);
        }
    }

    /**
     * UUID 유효성 검사
     * @param uuid : String
     */
    private void validateUuid(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            throw new CustomException(ErrorCode.ORDER_UUID_INVALID);
        }
    }

    /**
     * 거래 유형 유효한지 확인
     * @param market : String
     */
    private void validateMarket(String market) {
        if (market == null || market.trim().isEmpty()) {
            throw new CustomException(ErrorCode.ORDER_INVALID_MARKET);
        }
    }

    /**
     * 주문 쿼리 파라미터 생성
     * @param market : String
     * @param uuids : List<String>
     * @param page : Integer
     * @param limit :  Integer
     * @return : List<NameValuePair>
     */
    private List<NameValuePair> createOrderQueryParams(String market, List<String> uuids, Integer page, Integer limit,String state) {
        List<NameValuePair> queryParams = new ArrayList<>();
        queryParams.add(new BasicNameValuePair("market", market));
        queryParams.add(new BasicNameValuePair("limit", String.valueOf(limit != null ? limit : 100)));
        queryParams.add(new BasicNameValuePair("page", String.valueOf(page != null ? page : 1)));
        queryParams.add(new BasicNameValuePair("order_by", "desc"));
        queryParams.add(new BasicNameValuePair("state", state));

        if (uuids != null && !uuids.isEmpty()) {
            String uuidQuery = uuids.stream()
                    .map(uuid -> "uuids[]=" + uuid)
                    .collect(Collectors.joining("&"));
            queryParams.add(new BasicNameValuePair("uuids", uuidQuery));
        }

        return queryParams;
    }

    private OrderBookDTO.Market mapMarket(JsonNode marketNode) {
        if (marketNode == null) return null;

        return OrderBookDTO.Market.builder()
                .id(getText(marketNode, "id"))
                .name(getText(marketNode, "name"))
                .orderTypes(getStringList(marketNode, "order_types"))
                .askTypes(getStringList(marketNode, "ask_types"))
                .bidTypes(getStringList(marketNode, "bid_types"))
                .orderSides(getStringList(marketNode, "order_sides"))
                .bid(mapOrderConstraint(marketNode.get("bid")))
                .ask(mapOrderConstraint(marketNode.get("ask")))
                .maxTotal(getBigDecimal(marketNode, "max_total"))
                .state(getText(marketNode, "state"))
                .build();
    }

    private OrderBookDTO.OrderConstraint mapOrderConstraint(JsonNode constraintNode) {
        if (constraintNode == null) return null;

        return OrderBookDTO.OrderConstraint.builder()
                .currency(getText(constraintNode, "currency"))
                .priceUnit(getBigDecimal(constraintNode, "price_unit"))
                .minTotal(getBigDecimal(constraintNode, "min_total"))
                .build();
    }

    private OrderBookDTO.Account mapAccount(JsonNode accountNode) {
        if (accountNode == null) return null;

        return OrderBookDTO.Account.builder()
                .currency(getText(accountNode, "currency"))
                .balance(getBigDecimal(accountNode, "balance"))
                .locked(getBigDecimal(accountNode, "locked"))
                .avgBuyPrice(getBigDecimal(accountNode, "avg_buy_price"))
                .avgBuyPriceModified(getBoolean(accountNode))
                .unitCurrency(getText(accountNode, "unit_currency"))
                .build();
    }

    /**
     * 주문 파라미터 확인
     * @param market : String
     * @param side : String
     * @param volume : double
     * @param price : double
     * @param ordType : String
     */
    private void validateOrderParameters(String market, String side, double volume, double price, String ordType) {
        validateMarket(market);

        if (side == null || (!side.equals("bid") && !side.equals("ask"))) {
            throw new CustomException(ErrorCode.ORDER_INVALID_SIDE);
        }
        if (volume <= 0) {
            throw new CustomException(ErrorCode.ORDER_INVALID_VOLUME);
        }
        if (price <= 0) {
            throw new CustomException(ErrorCode.ORDER_INVALID_PRICE);
        }
        if (ordType == null || ordType.trim().isEmpty()) {
            throw new CustomException(ErrorCode.ORDER_INVALID_PARAMETERS);
        }
    }

    private OrderHistory buildOrderHistory(JsonNode responseNode) {
        return OrderHistory.builder()
                .uuid(getText(responseNode, "uuid"))
                .market(getText(responseNode, "market"))
                .side(getText(responseNode, "side"))
                .ordType(getText(responseNode, "ord_type"))
                .price(getBigDecimal(responseNode, "price"))
                .volume(getBigDecimal(responseNode, "volume"))
                .remainingVolume(getBigDecimal(responseNode, "remaining_volume"))
                .reservedFee(getBigDecimal(responseNode, "reserved_fee"))
                .remainingFee(getBigDecimal(responseNode, "remaining_fee"))
                .paidFee(getBigDecimal(responseNode, "paid_fee"))
                .locked(getBigDecimal(responseNode, "locked"))
                .executedVolume(getBigDecimal(responseNode, "executed_volume"))
                .tradesCount(responseNode.get("trades_count").asInt())
                .state(getText(responseNode, "state"))
                .build();
    }

    // Utility methods for safe JsonNode value extraction
    private BigDecimal getBigDecimal(JsonNode node, String fieldName) {
        JsonNode valueNode = node.get(fieldName);
        return valueNode != null && !valueNode.isNull() ?
                new BigDecimal(valueNode.asText()) : null;
    }

    private String getText(JsonNode node, String fieldName) {
        JsonNode valueNode = node.get(fieldName);
        return valueNode != null && !valueNode.isNull() ?
                valueNode.asText() : null;
    }

    private Boolean getBoolean(JsonNode node) {
        JsonNode valueNode = node.get("avg_buy_price_modified");
        return valueNode != null && !valueNode.isNull() ?
                valueNode.asBoolean() : null;
    }

    private List<String> getStringList(JsonNode node, String fieldName) {
        JsonNode arrayNode = node.get(fieldName);
        if (arrayNode == null || !arrayNode.isArray()) return new ArrayList<>();

        List<String> result = new ArrayList<>();
        arrayNode.elements().forEachRemaining(element ->
                result.add(element.asText())
        );
        return result;
    }


}