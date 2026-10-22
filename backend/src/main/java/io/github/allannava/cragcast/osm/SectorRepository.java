package io.github.allannava.cragcast.osm;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectorRepository extends JpaRepository<Sector, String> {

    List<Sector> findByCragIdOrderByPosition(long cragId);
}
