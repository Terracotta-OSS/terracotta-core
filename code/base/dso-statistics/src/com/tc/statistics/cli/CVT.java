/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.tc.util.ResourceBundleHelper;

import java.io.PrintWriter;
import java.util.Iterator;

public class CVT {
  private static final ResourceBundleHelper bundleHelper = new ResourceBundleHelper(CVT.class);

  private final CliCommands commands;
  private final Options options;

  public CVT() {
    commands = new CliCommands();
    
    options = new Options()
      .addOption("h", "help", false, bundleHelper.getString("option.help"))
      .addOption(OptionBuilder.hasArg()
        .withArgName("number")
        .withDescription(bundleHelper.getString("option.port"))
        .withLongOpt("port")
        .create("p"))
      .addOption(OptionBuilder.hasArg()
        .withArgName("hostname|ip")
        .withDescription(bundleHelper.getString("option.host"))
        .withLongOpt("host")
        .create("H"));
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
      commands.getConnection().setHost(cli.getOptionValue("host"));
    }
    if (cli.hasOption("p")) {
      commands.getConnection().setPort(Integer.parseInt(cli.getOptionValue("port")));
    }

    if (!commands.processCommandList(cli.getArgList())) {
      System.out.println();
      printHelp();
    }
  }

  private CommandLine parseCli(final String[] args) throws ParseException {
    CommandLineParser parser = new GnuParser();
    CommandLine cli = null;
    try {
      cli = parser.parse(options, args);
    } catch (MissingOptionException e) {
      System.out.println(bundleHelper.format("error.option.missing", new Object[] { e.getMessage() }));
      printHelp();
    }
    return cli;
  }

  private void printHelp() {
    PrintWriter writer = new PrintWriter(System.out);
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(writer, HelpFormatter.DEFAULT_WIDTH, "java " + CVT.class.getName() + " [OPTION]... [COMMAND [ARGUMENTS]]...", "Options:", options, HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, null);

    writer.println();
    writer.println("Commands:");
    for (Iterator it = commands.getSupportedCommands().iterator(); it.hasNext(); ) {
      CliCommand command = (CliCommand)it.next();
      StringBuffer buffer = new StringBuffer();
      buffer.append(" ");
      buffer.append(command.getCommandName());
      String[] argument_names = command.getArgumentNames();
      if (argument_names != null &&
          argument_names.length > 0) {
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