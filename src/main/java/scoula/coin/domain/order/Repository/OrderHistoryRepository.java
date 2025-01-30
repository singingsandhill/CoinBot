package scoula.coin.domain.order.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import scoula.coin.application.entity.OrderHistory;

import java.util.List;
import java.util.Optional;

public interface OrderHistoryRepository extends JpaRepository<OrderHistory, String> {
    List<OrderHistory> findByMarketOrderByCreatedAtDesc(String market);

    List<OrderHistory> findBySideOrderByCreatedAtDesc(String side);

    List<OrderHistory> findByStateOrderByCreatedAtDesc(String state);

    @Query(nativeQuery = true, value = "SELECT\n" +
            "  ( # 거래 후 남은 코인 평가액\n" +
            "    SELECT \n" +
            "      t1.total_volume * t2.latest_price AS multiplied_result FROM (\n" +
            "      -- 볼륨 총합\n" +
            "      SELECT  SUM(\n" +
            "\t\tCASE\n" +
            "\t\t\tWHEN side = 'bid' THEN volume\n" +
            "            WHEN side = 'ask' THEN -volume\n" +
            "            ELSE 0 END ) AS total_volume FROM coin.order_history\n" +
            "\t\t) AS t1\n" +
            "    CROSS JOIN (\n" +
            "      -- 가장 최근 price\n" +
            "      SELECT price AS latest_price FROM coin.order_history ORDER BY created_at DESC LIMIT 1\n" +
            "\t\t) AS t2        -- ★★★ 여기서 꼭 별칭을 줘야 함\n" +
            "  )\n" +
            "  +\n" +
            "  ( # 매매 금액 정산\n" +
            "    SELECT\n" +
            "      SUM(\n" +
            "        CASE \n" +
            "          WHEN side = 'bid' THEN - (price * volume) - remaining_fee\n" +
            "          WHEN side = 'ask' THEN ((price * volume) + remaining_fee)\n" +
            "          ELSE 0\n" +
            "        END\n" +
            "      ) AS total\n" +
            "    FROM coin.order_history\n" +
            "  )\n" +
            "-\n" +
            "  (select sum(remaining_fee) from coin.order_history) # 누락된 매도 수수료 계산\n" +
            "  AS final_result\n" +
            ";\n")
    Optional caclTradingResult();

    @Query(value = "select min(o.createdAt) from OrderHistory o")
    Optional getStartDate();

    @Query(value = "select count(*) from OrderHistory o where o.side='bid'")
    Optional getNumofBid();

    @Query(value = "select count(*) from OrderHistory o where o.side='ask'")
    Optional getNumofAsk();


}
