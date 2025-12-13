package ru.alxpro.scriptable_http_client_light;

import static ru.alxpro.scriptable_http_client_light.utils.FileUtils.getNameWithoutExt;
import static ru.alxpro.scriptable_http_client_light.utils.ScriptLoader.SCRIPT_KEY;

import java.io.File;
import java.security.CodeSource;
import java.util.List;
import ru.alxpro.scriptable_http_client_light.script.AppConfig;
import ru.alxpro.scriptable_http_client_light.script.Env;
import ru.alxpro.scriptable_http_client_light.script.JsLogger;
import ru.alxpro.scriptable_http_client_light.script.SharedContext;
import ru.alxpro.scriptable_http_client_light.script.Utils;
import ru.alxpro.scriptable_http_client_light.script.client.AuthClient;
import ru.alxpro.scriptable_http_client_light.script.client.RestClient;
import ru.alxpro.scriptable_http_client_light.script.client.SoapClient;
import ru.alxpro.scriptable_http_client_light.script.server.RestTestServer;
import ru.alxpro.scriptable_http_client_light.script.server.SoapTestServer;
import ru.alxpro.scriptable_http_client_light.utils.ArgsParser;
import ru.alxpro.scriptable_http_client_light.utils.CliHandler;
import ru.alxpro.scriptable_http_client_light.utils.ScriptLoader;
import ru.alxpro.scriptable_http_client_light.utils.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);
  private static final String CONFIG_FILE = "config.properties";
  private static String appHome = ".";
  private static ArgsParser parser;
  private static final SharedContext context = new SharedContext();

  public static void main(String[] args) {
    setupArgsParser(args);
    if (parser.hasOption("help")) {
      parser.printHelp();
      System.exit(0);
    }
    log.info("=== Application Started ===");
    detectAppHome();
    startApp();
    log.info("=== Application Stopped ===");
  }

  private static void setupArgsParser(String[] args) {
    parser = new ArgsParser();
    parser.setAppUsage("Usage: java -jar scriptable_http_client.jar [options] [id ...]");
    parser.addOption("h", "help", false, "Show this help message");
    parser.addOption("c", "config", true, "Path to properties file");
    parser.addOption("s", "scripts", true, "Override scripts dir");
    try {
      parser.parse(args);
    } catch (IllegalArgumentException e) {
      System.err.println("Args parse Error: " + e.getMessage());
      parser.printHelp();
      System.exit(1);
    }
  }

  private static void detectAppHome() {
    try {
      log.info("Current dir: {}", System.getProperty("user.dir"));
      CodeSource codeSource = Main.class.getProtectionDomain().getCodeSource();
      File uri = new File(codeSource.getLocation().toURI());
      if (uri.getName().endsWith(".jar")) {
        appHome = uri.getParent();
      } else {
        appHome = uri.getParentFile().getParent();
      }
      log.info("App dir: {}", appHome);
    } catch (Exception e) {
      log.warn("Could not detect App home: {}", e.getMessage());
    }
  }

  public static void loadConfig(AppConfig config) {
    File configFile = null;
    String optConfig = parser.getOption("config");
    if (optConfig != null) {
      configFile = new File(appHome, optConfig);
    }
    if (configFile == null) {
      configFile = new File(appHome, CONFIG_FILE);
    }
    String configPath = configFile.getAbsolutePath();
    if (!config.load(configPath)) {
      log.warn("File {} not found. Using defaults.", configPath);
    } else {
      log.info("App configuration loaded");
    }
  }

  public static File getDataDir() {
    File dataDir = new File(appHome, "data");
    if (!dataDir.exists()) {
      if (dataDir.mkdirs()) {
        log.info("Created data dir: {}", dataDir.getAbsolutePath());
      }
    }
    return dataDir;
  }

  private static void setupRegistry(
      ServiceRegistry registry, AppConfig config,
      RestTestServer restTestServer, SoapTestServer soapTestServer
  ) {
    registry.register("config", config);
    Utils utils = new Utils(context);
    registry.register("utils", utils);
    registry.register("auth", new AuthClient(config, utils));
    registry.register("http", new RestClient(config));
    registry.register("soap", new SoapClient(config, utils));
    registry.register("context", context);
    registry.register("env", new Env());
    registry.register("logger", new JsLogger());
    registry.register("restTestServer", restTestServer);
    registry.register("soapTestServer", soapTestServer);
  }

  private static void startApp() {
    var config = new AppConfig();
    loadConfig(config);

    var registry = new ServiceRegistry();
    var restTestServer = new RestTestServer();
    var soapTestServer = new SoapTestServer();
    setupRegistry(registry, config, restTestServer, soapTestServer);

    var loader = new ScriptLoader(registry);
    String cfgFolder = config.get("script.folder", "scripts");
    String folder = parser.getOption("scripts", cfgFolder);
    String scriptsPath = new File(appHome, folder).getAbsolutePath();
    loader.init(scriptsPath);

    List<String> scriptIds = parser.getPositionalArgs();
    if (!scriptIds.isEmpty()) {
      runBatchMode(loader, registry, scriptIds);
    } else {
      new CliHandler(loader, config, context, scriptsPath).start();
    }

    loader.stop();
    restTestServer.stop();
    soapTestServer.stop();
  }

  private static void runBatchMode(
      ScriptLoader loader, ServiceRegistry registry, List<String> scriptIds
  ) {
    log.info("=== Batch Mode Started ===");
    boolean success = true;
    try {
      for (String id : scriptIds) {
        if (!executeBatch(loader, registry, id)) {
          success = false;
          break;
        }
      }
    } catch (Exception e) {
      log.error("Batch Error: {}", e.getMessage(), e);
      success = false;
    }
    if (success) {
      log.info("=== Batch Completed ===");
    } else {
      log.error("=== Batch Failed ===");
    }
  }

  private static boolean executeBatch(
      ScriptLoader loader, ServiceRegistry registry, String key
  ) {
    ScriptLoader.ScriptEntry entry = loader.get(key);
    if (entry == null) {
      log.error("Script not found for: {}", key);
      return false;
    }
    JsLogger jsLogger = registry.getService("logger");
    jsLogger.reset();
    try {
      log.info("START [{}] File:[{}]", key, entry.name);
      context.put(SCRIPT_KEY, getNameWithoutExt(entry.name));
      entry.scenario.runScenario();
      log.info("END [{}] File:[{}]", key, entry.name);
    } catch (Exception e) {
      log.error("ERROR [{}]: {}", key, e.getMessage(), e);
      return false;
    }
    if (jsLogger.hasError()) {
      log.error("Script reported errors via JS logger");
      return false;
    }
    return true;
  }
}