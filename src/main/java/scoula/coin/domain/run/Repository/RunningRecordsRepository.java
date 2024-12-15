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

    @Query("SELECT r.name, count(r) from RunningRecords r GROUP BY r.name ORDER BY count(r) desc ")
    List<RunningRecords> findNameAndCountGroupByNameOrderByCountDesc();

    @Query("SELECT r.name, sum(r.distance) from RunningRecords r group by r.name ORDER BY sum(r.distance)")
    List<RunningRecords> findNameAndDistanceGroupByNameOrderByDistance(Long id);

}
