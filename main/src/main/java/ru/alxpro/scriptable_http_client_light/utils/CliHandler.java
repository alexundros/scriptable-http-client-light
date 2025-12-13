package ru.alxpro.scriptable_http_client_light.utils;

import static ru.alxpro.scriptable_http_client_light.utils.FileUtils.getNameWithoutExt;
import static ru.alxpro.scriptable_http_client_light.utils.ScriptLoader.SCRIPT_KEY;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import ru.alxpro.scriptable_http_client_light.Main;
import ru.alxpro.scriptable_http_client_light.script.AppConfig;
import ru.alxpro.scriptable_http_client_light.script.SharedContext;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliHandler {

  private static final Logger log = LoggerFactory.getLogger(CliHandler.class);
  private final ScriptLoader loader;
  private final AppConfig config;
  private final SharedContext context;
  private final String scriptsPath;
  private static final int DEFAULT_WIDTH = 120;
  private final Map<String, String> commandAliases = new HashMap<>();

  public CliHandler(
      ScriptLoader loader, AppConfig config, SharedContext ctx, String scriptsPath
  ) {
    this.loader = loader;
    this.config = config;
    this.context = ctx;
    this.scriptsPath = scriptsPath;

    registerCommand("list", "l");
    registerCommand("context", "c");
    registerCommand("reload", "r");
    registerCommand("exit", "e");
    registerCommand("help", "h");
  }

  private void registerCommand(String full, String alias) {
    commandAliases.put(full, full);   // list -> list
    commandAliases.put(alias, full);  // l -> list
  }

  private static void help() {
    log.info(
        "\nAvailable Commands:"
            + "\n [id] - start script"
            + "\n list    [l] - available scenarios"
            + "\n context [c] - current context"
            + "\n reload  [r] - reload"
            + "\n exit    [e] - exit from app"
            + "\n help    [h] - display help"
    );
  }

  public void start() {
    TerminalBuilder tb = TerminalBuilder.builder();
    try (Terminal terminal = tb
        .system(true).jna(true).exec(false).dumb(true)
        .build()) {
      Completer aggregateCompleter = new AggregateCompleter(
          new StringsCompleter(commandAliases.keySet()),
          (reader, line, candidates) -> {
            List<String> list = loader.getAvailableScriptsIds();
            new StringsCompleter(list).complete(reader, line, candidates);
          }
      );
      String dataPath = Main.getDataDir().getPath();
      LineReader reader = LineReaderBuilder.builder()
          .terminal(terminal)
          .completer(aggregateCompleter)
          .variable(LineReader.HISTORY_FILE, Path.of(dataPath, "history.txt"))
          .variable(LineReader.HISTORY_SIZE, 100)
          .build();

      log.info("Press <tab> for autocomplete");
      while (true) {
        String line;
        try {
          line = reader.readLine("> ").trim();
        } catch (UserInterruptException | EndOfFileException e) {
          log.error("Input stream closed. Exiting.");
          break;
        }
        if (!line.isEmpty()) {
          String cmd = commandAliases.getOrDefault(line.toLowerCase(), line);
          switch (cmd) {
            case "exit":
              return;
            case "list":
              int tw = terminal.getWidth();
              int width = tw > 10 ? tw : DEFAULT_WIDTH;
              String table = formatAsTable(loader.getAvailableScriptsList(), width);
              System.out.printf("Available Scenarios:%n%s", table);
              break;
            case "context":
              log.info("Current Context: {}", context.getAll());
              break;
            case "reload":
              log.info("Forcing reload...");
              Main.loadConfig(config);
              loader.loadAll(new java.io.File(scriptsPath));
              break;
            case "help":
              help();
              break;
            default:
              ScriptLoader.ScriptEntry entry = loader.get(line);
              if (entry != null) {
                startScript(line, entry);
              } else {
                log.warn("Unknown command or script: {}", line);
              }
          }
        }
      }
    } catch (Exception e) {
      log.error("Terminal error: {}", e.getMessage(), e);
    }
  }

  private String formatAsTable(List<String> items, int width) {
    if (items.isEmpty()) {
      return "(No scripts found)\n";
    }
    ToIntFunction<String> fn = s -> s == null ? 0 : s.trim().length();
    int maxLen = items.stream().mapToInt(fn).max().orElse(0);
    int colGap = 4;
    int colWidth = maxLen + colGap;
    int cols = Math.max(1, width / colWidth);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < items.size(); i++) {
      String item = items.get(i).trim();
      sb.append(String.format("%-" + colWidth + "s", item));
      if ((i + 1) % cols == 0 || i == items.size() - 1) {
        sb.append("\n");
      }
    }
    return sb.toString();
  }

  private void startScript(String key, ScriptLoader.ScriptEntry entry) {
    try {
      log.info("START [{}] File:[{}]", key, entry.name);
      context.put(SCRIPT_KEY, getNameWithoutExt(entry.name));
      entry.scenario.runScenario();
      log.info("END [{}] File:[{}]", key, entry.name);
    } catch (Exception e) {
      log.error("ERROR [{}]: {}", key, e.getMessage(), e);
    }
  }
}
