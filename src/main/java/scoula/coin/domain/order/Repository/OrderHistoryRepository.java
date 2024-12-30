package scoula.coin.domain.order.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import scoula.coin.application.entity.OrderHistory;

import java.util.List;

public interface OrderHistoryRepository extends JpaRepository<OrderHistory, String> {
    List<OrderHistory> findByMarketOrderByCreatedAtDesc(String market);
    List<OrderHistory> findBySideOrderByCreatedAtDesc(String side);
    List<OrderHistory> findByStateOrderByCreatedAtDesc(String state);
}
