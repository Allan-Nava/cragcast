package io.github.allannava.cragcast.history;

import java.time.LocalDate;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CragHistoryRepository extends JpaRepository<CragHistory, CragHistory.Key> {

    @Modifying
    @Query("delete from CragHistory h where h.key.day in :days")
    int deleteByDayIn(@Param("days") Collection<LocalDate> days);
}
