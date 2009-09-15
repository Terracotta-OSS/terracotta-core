/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

import com.tc.util.ResourceBundleHelper;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CVT {
  private final static ResourceBundleHelper BUNDLE_HELPER           = new ResourceBundleHelper(CVT.class);
  private final static Pattern              SCRIPT_COMMANDS_PATTERN = Pattern
                                                                        .compile(
                                                                                 "(?:(?<=\\s+\")[^\"]++(?=\"\\s+))|(?:[^\\s\"]+)",
                                                                                 Pattern.MULTILINE);
  private final static Pattern              STRIP_NEWLINES          = Pattern.compile("[\n\r]", Pattern.MULTILINE);

  private final CliCommands                 commands;
  private final Options                     options;

  public CVT() {
    commands = new CliCommands();

    options = new Options();
    options.addOption("h", "help", false, BUNDLE_HELPER.getString("option.help"));
    options.addOption(createOption("number", "option.port", "port", "p"));
    options.addOption(createOption("hostname|ip", "option.host", "host", "H"));
    options.addOption(createOption("filename", "option.file", "file", "f"));
    options.addOption("u", "username", true, "username");
    options.addOption("w", "password", true, "password");
  }

  private static Option createOption(String argName, String descString, String longOpt, String opt) {
    OptionBuilder.hasArg();
    OptionBuilder.withArgName(argName);
    OptionBuilder.withDescription(BUNDLE_HELPER.getString(descString));
    OptionBuilder.withLongOpt(longOpt);
    return OptionBuilder.create(opt);
  }

  private static String readPassword() {
    try {
      System.out.print("Enter password: ");
      return new jline.ConsoleReader().readLine(Character.valueOf('*'));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void run(final String[] args) throws Exception {
    CommandLine cli = parseCli(args);
    if (null == cli) {
      System.exit(1);
    }

    if (cli.hasOption("h")) {
      printHelp();
      return;
    }

    if (cli.hasOption("H")) {
      commands.getConnection().setHost(cli.getOptionValue("H"));
    }
    if (cli.hasOption("p")) {
      commands.getConnection().setPort(Integer.parseInt(cli.getOptionValue("p")));
    }
    if (cli.hasOption("u")) {
      commands.getConnection().setUsername(cli.getOptionValue("u"));
      String password = null;
      if (cli.hasOption("w")) {
        password = cli.getOptionValue("w");
      } else {
        password = readPassword();
      }
      System.out.println("Password: " + password);
      commands.getConnection().setPassword(password);
    }

    // create the commands to process
    List aggregated_commands = new ArrayList();

    if (cli.hasOption("f")) {
      String filename = cli.getOptionValue("f");
      if (!extractCommandsFromScript(aggregated_commands, filename)) { return; }
    }
    aggregated_commands.addAll(cli.getArgList());

    if (!commands.processCommandList(aggregated_commands)) {
      System.out.println();
      printHelp();
    }
  }

  private boolean extractCommandsFromScript(List aggregated_commands, String filename) throws IOException {
    File script_file = new File(filename);
    if (!script_file.exists()) {
      System.out.println("> The script file '" + filename + "' couldn't be found.");
      return false;
    } else if (!script_file.isFile()) {
      System.out.println("> The script at '" + filename + "' is not a file.");
      return false;
    } else if (!script_file.canRead()) {
      System.out.println("> The script file at '" + filename + "' is not readable.");
      return false;
    } else {
      String script = FileUtils.readFileToString(script_file, "ISO-8859-1");
      Matcher script_matcher = SCRIPT_COMMANDS_PATTERN.matcher(script);
      while (script_matcher.find()) {
        String script_command = script_matcher.group();
        script_command = STRIP_NEWLINES.matcher(script_command).replaceAll("");
        aggregated_commands.add(script_command);
      }
    }
    return true;
  }

  private CommandLine parseCli(final String[] args) throws ParseException {
    CommandLineParser parser = new GnuParser();
    CommandLine cli = null;
    try {
      cli = parser.parse(options, args);
    } catch (MissingArgumentException e) {
      System.out.println(BUNDLE_HELPER.format("error.argument.missing", new Object[] { e.getMessage() }));
      printHelp();
    } catch (MissingOptionException e) {
      System.out.println(BUNDLE_HELPER.format("error.option.missing", new Object[] { e.getMessage() }));
      printHelp();
    }
    return cli;
  }

  private void printHelp() {
    PrintWriter writer = new PrintWriter(System.out);
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(writer, HelpFormatter.DEFAULT_WIDTH, "java " + CVT.class.getName()
                                                             + " [OPTION]... [COMMAND [ARGUMENTS]]...", "Options:",
                        options, HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, null);

    writer.println();
    writer.println("Commands:");
    for (Iterator it = commands.getSupportedCommands().iterator(); it.hasNext();) {
      CliCommand command = (CliCommand) it.next();
      StringBuffer buffer = new StringBuffer();
      buffer.append(" ");
      buffer.append(command.getCommandName());
      String[] argument_names = command.getArgumentNames();
      if (argument_names != null && argument_names.length > 0) {
        for (int i = 0; i < argument_names.length; i++) {
          buffer.append(" <");
          buffer.append(argument_names[i]);
          buffer.append(">");
        }
      }
      writer.println(buffer.toString());
    }
    writer.close();
  }

  public static void main(final String[] args) throws Exception {
    new CVT().run(args);
  }
}
