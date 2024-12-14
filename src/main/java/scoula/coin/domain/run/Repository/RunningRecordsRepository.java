package scoula.coin.domain.run.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import scoula.coin.application.dto.RunnerDistanceDTO;
import scoula.coin.application.entity.RunningRecords;

import java.util.List;

@Repository
public interface RunningRecordsRepository extends JpaRepository<RunningRecords, Long> {
    @Query("SELECT new scoula.coin.application.dto.RunnerDistanceDTO(r.name, SUM(r.distance)) " +
            "FROM RunningRecords r GROUP BY r.name")
    List<RunnerDistanceDTO> findTotalDistanceByRunner();

}
