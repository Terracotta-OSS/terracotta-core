package com.tc.server;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.terracotta.config.ConfigurationProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.tc.server.CommandLineParser.Opt.CONSISTENT_STARTUP;
import static com.tc.server.CommandLineParser.Opt.HELP;
import static com.tc.server.CommandLineParser.Opt.SERVER_NAME;

class CommandLineParser {

  enum Opt {
    SERVER_NAME("n", "name"),
    CONSISTENT_STARTUP("c", "consistency-on-startup"),
    HELP("h", "help");

    String shortName;
    String longName;

    Opt(String shortName, String longName) {
      Objects.requireNonNull(shortName);
      Objects.requireNonNull(longName);
      this.shortName = shortName;
      this.longName = longName;
    }

    public String getShortName() {
      return shortName;
    }

    public String getLongName() {
      return longName;
    }

    public String getShortOption() {
      return "-" + shortName;
    }

    public String getLongOption() {
      return "--" + longName;
    }

    public boolean same(String optionName) {
      return getShortOption().equals(optionName) || getLongOption().equals(optionName);
    }
  }

  private final String serverName;

  private final boolean consistentStartup;

  private final List<String> providerArgs = new ArrayList<>();

  CommandLineParser(String[] args, ConfigurationProvider configurationProvider) {
    Set<Integer> commonArgIndexes = getCommonArgIndexes(args);

    List<String> commonArgs = new ArrayList<>();

    for (int i = 0; i < args.length; i++) {
      if (commonArgIndexes.contains(i)) {
        commonArgs.add(args[i]);
      } else {
        providerArgs.add(args[i]);
      }
    }

    try {
      CommandLine commandLine = new DefaultParser().parse(createOptions(), commonArgs.toArray(new String[0]));

      if (commandLine.hasOption('h')) {
        printHelp(configurationProvider);
        System.exit(0);
      }

      this.serverName = commandLine.getOptionValue(SERVER_NAME.getShortName());
      this.consistentStartup = commandLine.hasOption(CONSISTENT_STARTUP.getShortName());
    } catch (ParseException pe) {
      throw new RuntimeException("Unable to parse command-line arguments: " + Arrays.toString(args), pe);
    }
  }

  String getServerName() {
    return this.serverName;
  }

  boolean consistentStartup() {
    return this.consistentStartup;
  }

  List<String> getProviderArgs() {
    return Collections.unmodifiableList(providerArgs);
  }

  private static void printHelp(ConfigurationProvider configurationProvider) {
    new HelpFormatter().printHelp(
        "[start-tc-server.sh|bat] [options]",
        "Options: " + System.lineSeparator(),
        createOptions(),
        ""
    );
    System.out.println(configurationProvider.getConfigurationParamsDescription());
  }

  private static Options createOptions() {
    Options options = new Options();

    options.addOption(
        Option.builder(SERVER_NAME.getShortName())
              .longOpt(SERVER_NAME.getLongName())
              .hasArg()
              .argName("server-name")
              .desc("specifies the server name, defaults to the host name")
              .build()
    );

    options.addOption(
        Option.builder(CONSISTENT_STARTUP.getShortName())
              .longOpt(CONSISTENT_STARTUP.getLongName())
              .desc("ensure that data consistency is preserved on startup")
              .build()
    );

    options.addOption(
        Option.builder(HELP.getShortName())
              .longOpt(HELP.getLongName())
              .build()
    );

    return options;
  }

  private static Set<Integer> getCommonArgIndexes(String[] args) {
    Set<Integer> filteredArgs = new HashSet<>();

    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (SERVER_NAME.same(arg)) {
        filteredArgs.add(i);
        if (i + 1 < args.length) {
          filteredArgs.add(i + 1);
          i++;
        }
      } else if (CONSISTENT_STARTUP.same(arg) || HELP.same(arg)) {
        filteredArgs.add(i);
      }
    }

    return filteredArgs;
  }
}
