package scoula.coin.domain.run.Repository;

import org.springframework.data.repository.CrudRepository;
import scoula.coin.application.entity.TradingSignalHistory;

import java.util.List;
import java.util.Optional;

public interface TradingSignalHistoryRepository extends CrudRepository<TradingSignalHistory, Integer> {
    List<TradingSignalHistory> findByMarketOrderByCreatedAtDesc(String market);
    List<TradingSignalHistory> findBySignalTypeOrderByCreatedAtDesc(Integer signalType);
    List<TradingSignalHistory> findByOrderExecutedOrderByCreatedAtDesc(Boolean orderExecuted);

    Optional<TradingSignalHistory> findByOrderUuid(String orderUuid);
}
