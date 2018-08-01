package org.obiba.datasource.opal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONObject;
import org.obiba.datasource.opal.support.REDCapDatasourceFactory;
import org.obiba.magma.DatasourceFactory;
import org.obiba.opal.spi.datasource.DatasourceService;
import org.obiba.opal.spi.datasource.DatasourceUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class REDCapDatasourceService implements DatasourceService {

  private static final Logger log = LoggerFactory.getLogger(REDCapDatasourceService.class);

  private static final String SCHEMA_FILE_EXT = ".json";

  private Properties properties;
  private boolean running;
  private Collection<DatasourceUsage> usages = new HashSet<>();

  @Override
  public String getName() {
    return "datasource-redcap";
  }

  @Override
  public Properties getProperties() {
    return properties;
  }

  @Override
  public void configure(Properties properties) {
    this.properties = properties;
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  @Override
  public void start() {
    initUsages();
    running = true;
  }

  @Override
  public void stop() {
    running = false;
  }

  @Override
  public Collection<DatasourceUsage> getUsages() {
    return usages;
  }

  @Override
  public JSONObject getJSONSchemaForm(DatasourceUsage usage) {
    JSONObject jsonObject = new JSONObject();

    try {
      jsonObject = new JSONObject(readUsageSchema(usage));
    } catch (IOException e) {
      log.error("Error reading usage jsonSchema: %s", e.getMessage());
    }

    return jsonObject;
  }

  @Override
  public DatasourceFactory createDatasourceFactory(DatasourceUsage usage, JSONObject parameters) {
    REDCapDatasourceFactory exampleDatasourceFactory = new REDCapDatasourceFactory();
    exampleDatasourceFactory.setName(parameters.optString("name"));

    return exampleDatasourceFactory;
  }

  private void initUsages() {
    String usagesString = properties.getProperty("usages", "").trim();
    usages = usagesString.isEmpty() ? new HashSet<>()
        : Stream.of(usagesString.split(",")).map(usage -> DatasourceUsage.valueOf(usage.trim().toUpperCase()))
            .collect(Collectors.toSet());
  }

  private Path getUsageSchemaPath(DatasourceUsage usage) {
    return Paths.get(properties.getProperty(INSTALL_DIR_PROPERTY), usage.name().toLowerCase() + SCHEMA_FILE_EXT);
  }

  private boolean hasUsage(DatasourceUsage usage) {
    return getUsages().stream().filter(u -> u.equals(usage)).count() == 1;
  }

  private String readUsageSchema(DatasourceUsage usage) throws IOException {
    Path usageSchemaPath = getUsageSchemaPath(usage).toAbsolutePath();
    String result = "{}";

    log.info("Reading usage jsonSchema at %s", usageSchemaPath);

    if (hasUsage(usage) && usageSchemaPath.toFile().exists()) {
      String schema = Files.lines(usageSchemaPath).reduce("", String::concat).trim();
      if (!schema.isEmpty())
        result = schema;
    }

    return result;
  }

}