package scoula.coin.domain.run.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import scoula.coin.application.entity.Regular;

import java.util.List;

public interface RegularRepository extends JpaRepository<Regular, Long> {

    @Query(value = "SELECT r.name, COUNT(r.id) AS cnt " +
            "FROM run.regular r " +
            "GROUP BY r.name " +
            "HAVING cnt = (SELECT MAX(counts.cnt) " +
            "FROM (SELECT COUNT(r2.id) AS cnt " +
            "FROM run.regular r2 " +
            "GROUP BY r2.name) counts)",
            nativeQuery = true)
    List<Object> findByNameCount();


    @Query("select r.name, count(r) from Regular r group by r.name order by count(r),r.name desc")
    List<Object> findAllof();


}
