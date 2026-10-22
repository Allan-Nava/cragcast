package io.github.allannava.cragcast.osm;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, String> {

    /** Easiest first; PostgreSQL puts the ungraded last. */
    List<Route> findByCragIdOrderByGradeRankAscNameAscIdAsc(long cragId);

    long countByCragIdIsNull();
}
