package io.github.allannava.cragcast.crag;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CragRepository extends JpaRepository<Crag, Long> {

    Optional<Crag> findBySlug(String slug);

    List<Crag> findAllByOrderByNameAsc();
}
