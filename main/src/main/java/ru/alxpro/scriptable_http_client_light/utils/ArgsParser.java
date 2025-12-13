package ru.alxpro.scriptable_http_client_light.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArgsParser {

  private final List<Option> definedOpts = new ArrayList<>();
  private final Map<String, String> parsedOpts = new HashMap<>();
  private final List<String> positionalArgs = new ArrayList<>();
  private String appUsage = "Usage: java -jar app.jar";

  private static class Option {

    String shortName;
    String longName;
    boolean hasValue;
    String desc;

    Option(
        String shortName, String longName, boolean hasValue, String desc
    ) {
      this.shortName = shortName;
      this.longName = longName;
      this.hasValue = hasValue;
      this.desc = desc;
    }
  }

  public void setAppUsage(String appUsage) {
    this.appUsage = appUsage;
  }

  public void addOption(
      String shortName, String longName, boolean hasValue, String desc
  ) {
    definedOpts.add(new Option(shortName, longName, hasValue, desc));
  }

  public void parse(String[] args) {
    parsedOpts.clear();
    positionalArgs.clear();
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (arg.startsWith("-")) {
        i = parseOption(arg, args, i);
      } else {
        positionalArgs.add(arg);
      }
    }
  }

  private int parseOption(String arg, String[] args, int currIdx) {
    String key = arg.replaceAll("^-+", "");
    String value = null;
    if (key.contains("=")) {
      String[] parts = key.split("=", 2);
      key = parts[0];
      value = parts[1];
    }
    Option opt = findOption(key);
    if (opt == null) {
      throw new IllegalArgumentException("Unknown option: " + arg);
    }
    if (opt.hasValue) {
      if (value == null) {
        if (currIdx + 1 < args.length && !args[currIdx + 1].startsWith("-")) {
          return processOptionValue(opt, args[currIdx + 1], currIdx + 1);
        } else {
          throw new IllegalArgumentException("Option " + arg + " requires a value");
        }
      } else {
        parsedOpts.put(opt.longName, value);
      }
    } else {
      parsedOpts.put(opt.longName, "true");
    }
    return currIdx;
  }

  private int processOptionValue(Option opt, String value, int idx) {
    parsedOpts.put(opt.longName, value);
    return idx;
  }

  private Option findOption(String key) {
    for (Option opt : definedOpts) {
      if (key.equals(opt.shortName) || key.equals(opt.longName)) {
        return opt;
      }
    }
    return null;
  }

  public boolean hasOption(String longName) {
    return parsedOpts.containsKey(longName);
  }

  public String getOption(String longName) {
    return parsedOpts.get(longName);
  }

  public String getOption(String longName, String defVal) {
    return parsedOpts.getOrDefault(longName, defVal);
  }

  public List<String> getPositionalArgs() {
    return positionalArgs;
  }

  public void printHelp() {
    System.out.println(appUsage + "\nOptions:");
    for (Option opt : definedOpts) {
      String shortOpt = opt.shortName != null && !opt.shortName.isEmpty()
          ? "-" + opt.shortName + "," : "   ";
      String longOpt = "--" + opt.longName;
      String valHint = opt.hasValue ? " <arg>" : "";
      System.out.printf("  %s %-20s %s%n", shortOpt, longOpt + valHint, opt.desc);
    }
    System.out.println();
  }
}
