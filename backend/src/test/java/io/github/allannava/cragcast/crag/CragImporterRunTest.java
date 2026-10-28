package io.github.allannava.cragcast.crag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import tools.jackson.databind.json.JsonMapper;

/**
 * CG-49: the startup import over the real data/crags.json, against a mocked repository —
 * new crags saved, known ones updated in place, none deleted. The Testcontainers tests cover
 * the same against PostgreSQL in CI.
 */
class CragImporterRunTest {

    @Test
    void insertsTheNewAndUpdatesTheKnown() throws Exception {
        CragRepository repo = mock(CragRepository.class);
        var importer = new CragImporter(repo, JsonMapper.builder().build());
        var entries = importer.read();
        var first = entries.getFirst();
        Crag known = new Crag(first.id(), "an old name", null, 0, 0, null, "https://example.org/old");
        when(repo.findBySlug(anyString())).thenReturn(Optional.empty());
        when(repo.findBySlug(first.id())).thenReturn(Optional.of(known));

        importer.run(new DefaultApplicationArguments());

        verify(repo, times(entries.size() - 1)).save(any(Crag.class));
        verify(repo, never()).delete(any());
        assertThat(known.getName()).isEqualTo(first.name());
        assertThat(known.getLatitude()).isEqualTo(first.latitude());
        assertThat(known.getElevation()).isEqualTo(first.elevation());
        // CG-43: every crag of the file says its region, and the table keeps it.
        assertThat(known.getRegion()).isEqualTo(first.region());
        assertThat(entries).allSatisfy(e -> assertThat(e.region())
                .isIn("Lombardia", "Piemonte", "Trentino-Alto Adige"));
    }
}
