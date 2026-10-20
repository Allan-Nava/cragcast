package io.github.allannava.cragcast.osm;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OsmObjectRepository extends JpaRepository<OsmObject, String> {

    List<OsmObject> findByCategoryOrderById(String category);

    long countByCategory(String category);
}
