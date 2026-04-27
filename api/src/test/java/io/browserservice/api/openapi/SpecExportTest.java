package io.browserservice.api.openapi;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Exports the OpenAPI spec generated from the controller annotations to {@code
 * openapi/generated.yaml} and enforces that the committed copy matches.
 *
 * <p>Regenerate via: {@code mvn test -pl api -Dtest=SpecExportTest -Dopenapi.update=true}.
 *
 * <p>The fetched spec is canonicalized (maps deep-sorted) before writing or comparing so that
 * non-deterministic map ordering inside springdoc doesn't cause spurious diffs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "browserservice.selenium.urls=http://localhost:4444/wd/hub",
      "browserservice.appium.urls=",
      "browserservice.browserstack.enabled=false"
    })
class SpecExportTest {

  private static final String SPEC_PATH = "openapi/generated.yaml";
  private static final String UPDATE_PROPERTY = "openapi.update";

  @Autowired private MockMvc mvc;

  @Test
  void committedOpenApiSpecMatchesCurrent() throws Exception {
    String fetched =
        mvc.perform(MockMvcRequestBuilders.get("/v3/api-docs.yaml"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    String current = canonicalize(fetched);

    Path committed = resolveCommittedPath();
    boolean update = Boolean.getBoolean(UPDATE_PROPERTY);

    if (update || !Files.exists(committed)) {
      Files.createDirectories(committed.getParent());
      Files.writeString(committed, current, StandardCharsets.UTF_8);
      if (!update) {
        Assertions.fail(
            "committed "
                + SPEC_PATH
                + " was missing; wrote current spec. "
                + "Re-run the build to verify.");
      }
      return;
    }

    String onDisk = canonicalize(Files.readString(committed, StandardCharsets.UTF_8));
    Assertions.assertEquals(
        onDisk,
        current,
        SPEC_PATH
            + " is out of date. Regenerate with: "
            + "mvn test -pl api -Dtest=SpecExportTest -D"
            + UPDATE_PROPERTY
            + "=true");
  }

  private static String canonicalize(String yaml) {
    Yaml loader = new Yaml();
    Object root = loader.load(yaml);
    Object sorted = sortMaps(root);

    DumperOptions opts = new DumperOptions();
    opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    opts.setIndent(2);
    opts.setPrettyFlow(true);
    opts.setSplitLines(false);
    Yaml dumper = new Yaml(opts);
    return dumper.dump(sorted);
  }

  @SuppressWarnings("unchecked")
  private static Object sortMaps(Object node) {
    if (node instanceof Map<?, ?> map) {
      TreeMap<String, Object> sorted = new TreeMap<>();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        sorted.put(String.valueOf(entry.getKey()), sortMaps(entry.getValue()));
      }
      return sorted;
    }
    if (node instanceof List<?> list) {
      List<Object> copy = new ArrayList<>(list.size());
      for (Object item : list) {
        copy.add(sortMaps(item));
      }
      return copy;
    }
    return node;
  }

  private static Path resolveCommittedPath() {
    Path cwd = Path.of("").toAbsolutePath();
    Path sibling = cwd.getParent() == null ? null : cwd.getParent().resolve(SPEC_PATH);
    if (cwd.getFileName() != null
        && "api".equals(cwd.getFileName().toString())
        && sibling != null) {
      return sibling;
    }
    return cwd.resolve(SPEC_PATH);
  }
}
