package io.github.allannava.cragcast.export;

import io.github.allannava.cragcast.ferrata.FerrataClient;
import io.github.allannava.cragcast.ferrata.FerrataTops;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * CG-115: data/ferrate.json as it ships in the jar (ADR 0023) — the tops the hourly refresh
 * asks the forecast for, and the via ferratas {@link SiteData} scores into the Pi's page and API.
 * Without the file, none: the Pi's page then reads as before CG-114.
 */
@Component
public class FerrateData implements FerrataTops {

    private final FerrateExport.FerrateFile file;

    FerrateData() throws IOException {
        this(read());
    }

    FerrateData(FerrateExport.FerrateFile file) {
        this.file = file;
    }

    static FerrateExport.FerrateFile none() {
        return new FerrateExport.FerrateFile(null, null, null, null, List.of());
    }

    private static FerrateExport.FerrateFile read() throws IOException {
        var resource = new ClassPathResource("data/ferrate.json");
        if (!resource.exists()) {
            return none();
        }
        try (InputStream in = resource.getInputStream()) {
            return JsonMapper.builder().build().readValue(in, FerrateExport.FerrateFile.class);
        }
    }

    FerrateExport.FerrateFile file() {
        return file;
    }

    @Override
    public Map<String, FerrataClient.Top> tops() {
        Map<String, FerrataClient.Top> out = new LinkedHashMap<>();
        for (FerrateExport.Ferrata f : file.ferrate()) {
            out.put(f.id(), FerrateExport.top(f));
        }
        return out;
    }
}
