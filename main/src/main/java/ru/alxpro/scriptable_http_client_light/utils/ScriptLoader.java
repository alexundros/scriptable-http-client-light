package ru.alxpro.scriptable_http_client_light.utils;

import static ru.alxpro.scriptable_http_client_light.utils.FileUtils.getNameWithoutExt;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptLoader {

  private static final Logger log = LoggerFactory.getLogger(ScriptLoader.class);
  private final Map<String, ScriptEntry> scriptMap = new ConcurrentHashMap<>();
  private final ScriptEngine engine;
  private Thread watcherThread;
  private volatile boolean running = true;
  private final Pattern numPattern = Pattern.compile("^(\\d+)");

  // Regex to strip comments (Block /*...*/ and Line //...)
  // Note: This simple regex might consume URLs in strings like "http://",
  // but for the purpose of heuristic checking "getName" it is acceptable.
  private static final Pattern COMMENT_PATTERN = Pattern.compile(
      "//.*|/\\*([\\S\\s]+?)\\*/"
  );

  // Regex to find definitions of getName or run.
  // Matches:
  // 1. Property:   getScenarioName : function
  // 2. Assignment: getScenarioName = function
  // 3. Function:   function getScenarioName()
  // 4. Quoted:     "getScenarioName" : ...
  private static final Pattern DEF_PATTERN = Pattern.compile(
      "(?x)" +  // Enable comments/whitespace mode in regex
          "(?:" +
          // Match: identifier followed by ':','=','('
          "\\b(getScenarioName|runScenario)\\s*[:=(]" +
          "|" +
          // Match: "identifier" followed by ':'
          "[\"'](getScenarioName|runScenario)[\"']\\s*:" +
          "|" +
          // Match: function identifier
          "function\\s+(getScenarioName|runScenario)\\b" +
          ")"
  );

  public static final String SCRIPT_KEY = "SCRIPT_KEY";

  public interface Scenario {

    default String getScenarioName() {
      return null;
    }

    void runScenario() throws ScriptException;
  }

  public static class ScriptEntry {

    public final String key;
    public final String name;
    public final Scenario scenario;

    public ScriptEntry(String key, String name, Scenario scenario) {
      this.key = key;
      this.name = name;
      this.scenario = scenario;
    }

    @Override
    public String toString() {
      String sn = scenario.getScenarioName();
      return String.format(sn != null ? "[%s] %s \"" + sn + "\"" : "[%s] %s", key, name);
    }
  }

  public ScriptLoader(ServiceRegistry registry) {
    var manager = new ScriptEngineManager();
    this.engine = manager.getEngineByName("javascript");
    if (this.engine == null) {
      log.error("CRITICAL: JS Engine Nashorn not found.");
      System.exit(1);
    }
    registry.getServices().forEach(engine::put);
  }

  public void init(String scriptsPath) {
    File scriptsDir = new File(scriptsPath);
    if (!scriptsDir.exists() && !scriptsDir.mkdirs()) {
      log.error("Could not create scripts dir: {}", scriptsPath);
      return;
    }
    loadAll(scriptsDir);
    startWatcher(scriptsPath);
  }

  public ScriptEntry get(String key) {
    return scriptMap.get(key);
  }

  public List<String> getAvailableScriptsList() {
    var sorted = new ArrayList<>(scriptMap.values());
    // Sort numerically if keys are numbers
    sorted.sort(Comparator.comparing(e -> {
      try {
        return Integer.parseInt(e.key);
      } catch (NumberFormatException ex) {
        return Integer.MAX_VALUE;
      }
    }));
    var result = new ArrayList<String>();
    sorted.forEach(e -> result.add(e.toString()));
    return result;
  }

  public List<String> getAvailableScriptsIds() {
    List<String> result = new ArrayList<>(scriptMap.keySet());
    scriptMap.values().forEach(it -> result.add(it.key));
    return result;
  }

  public void stop() {
    running = false;
    if (watcherThread != null) {
      watcherThread.interrupt();
    }
  }

  public void loadAll(File scriptsDir) {
    scriptMap.clear();
    FilenameFilter flt = (d, n) -> n.toLowerCase().endsWith(".js");
    File[] files = scriptsDir.listFiles(flt);
    if (files != null) {
      for (File f : files) {
        loadFile(f);
      }
    }
    Stream<String> s = scriptMap.values().stream().map(ScriptEntry::toString);
    log.info("Loaded Scripts: {}", s.collect(Collectors.joining(", ")));
  }

  private boolean isScenarioLookAlike(String content) {
    String clean = COMMENT_PATTERN.matcher(content).replaceAll("");
    Matcher match = DEF_PATTERN.matcher(clean);
    boolean hasGetName = false, hasRun = false;
    while (match.find()) {
      // Group 1: Unquoted property/variable (getName)
      // Group 2: Quoted property ("getName")
      // Group 3: Function declaration (function getName)
      String matched = match.group(1) != null ? match.group(1) :
          (match.group(2) != null ? match.group(2) : match.group(3));
      if ("getScenarioName".equals(matched)) {
        hasGetName = true;
      }
      if ("runScenario".equals(matched)) {
        hasRun = true;
      }
    }
    return hasGetName && hasRun;
  }

  private void loadFile(File file) {
    String name = file.getName();
    try {
      Matcher match = numPattern.matcher(name);
      // Use regex group as key, otherwise use file name without ext
      String key = match.find() ? match.group(1) : getNameWithoutExt(name);
      String content = Files.readString(file.toPath());
      CompiledScript compiled = ((Compilable) engine).compile(content);
      Scenario scenario = null;
      if (isScenarioLookAlike(content)) {
        Invocable invocable = (Invocable) engine;
        scenario = invocable.getInterface(compiled.eval(), Scenario.class);
        if (scenario != null) {
          scriptMap.put(key, new ScriptEntry(key, name, scenario));
        }
      }
      if (scenario == null) {
        scriptMap.put(key, new ScriptEntry(key, name, compiled::eval));
      }
    } catch (Exception e) {
      log.error("Error loading {}: {}", name, e.getMessage());
    }
  }

  private void startWatcher(String scriptsPath) {
    watcherThread = new Thread(() -> {
      try (var watcher = FileSystems.getDefault().newWatchService()) {
        Path path = Paths.get(scriptsPath);
        path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
        while (running) {
          WatchKey key = watcher.take();
          for (WatchEvent<?> event : key.pollEvents()) {
            Path changed = (Path) event.context();
            if (changed.toString().endsWith(".js")) {
              log.info("File change detected: {}", changed);
              loadFile(path.resolve(changed).toFile());
            }
          }
          key.reset();
        }
      } catch (Exception e) {
        if (running) {
          log.error("Watcher error", e);
        }
      }
    });
    watcherThread.setDaemon(true);
    watcherThread.start();
  }
}