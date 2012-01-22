/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

import com.tc.management.JMXConnectorProxy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXConnector;

public class CommandLineBuilder {
  private Options        options = new Options();
  private final String   callingClassName;
  private final String[] cmdArguments;
  private CommandLine    commandLine;
  private String         usageMessage;

  static {
    System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
  }

  public CommandLineBuilder(String callingClassName, String[] cmdArguments) {
    this.callingClassName = callingClassName;
    this.cmdArguments = Arrays.asList(cmdArguments).toArray(new String[0]);
  }

  public void setOptions(Options options) {
    this.options = options;
  }

  public void setUsageMessage(String message) {
    this.usageMessage = message;
  }

  public void addOption(String opt, String longOpt, boolean hasArg, String description, Class type, boolean isRequired) {
    Option option = new Option(opt, longOpt, hasArg, description);
    option.setType(type);
    option.setRequired(isRequired);
    options.addOption(option);
  }

  public void addOption(String opt, String description, Class type, boolean isRequired) {
    Option option = new Option(opt, description);
    option.setType(type);
    option.setRequired(isRequired);

    options.addOption(option);
  }

  public void addOption(String opt, boolean hasArg, String description, Class type, boolean isRequired) {
    Option option = new Option(opt, hasArg, description);
    option.setType(type);
    option.setRequired(isRequired);

    options.addOption(option);
  }

  public void addOption(String opt, String longOpt, boolean hasArg, String description, Class type, boolean isRequired,
                        String argName) {
    Option option = new Option(opt, longOpt, hasArg, description);
    option.setType(type);
    option.setRequired(isRequired);
    option.setArgName(argName);

    options.addOption(option);
  }

  public void addOption(String opt, String description, Class type, boolean isRequired, String argName) {
    Option option = new Option(opt, description);
    option.setType(type);
    option.setRequired(isRequired);
    option.setArgName(argName);

    options.addOption(option);
  }

  public void addOption(String opt, boolean hasArg, String description, Class type, boolean isRequired, String argName) {
    Option option = new Option(opt, hasArg, description);
    option.setType(type);
    option.setRequired(isRequired);
    option.setArgName(argName);

    options.addOption(option);
  }

  public String[] getArguments() {
    return commandLine.getArgs();
  }

  public void parse() {
    try {
      commandLine = new GnuParser().parse(options, cmdArguments);
    } catch (UnrecognizedOptionException e) {
      System.err.println(e.getMessage());
      usageAndDie();
    } catch (ParseException e) {
      System.err.println(e.getMessage());
      usageAndDie();
    }
  }

  public void usageAndDie() {
    String message = usageMessage != null ? usageMessage : "java " + callingClassName;
    new HelpFormatter().printHelp(message, options);
    System.exit(1);
  }

  public void usageAndDie(String message) {
    new HelpFormatter().printHelp(message, options);
    System.exit(1);
  }

  public void printArguments() {
    System.err.println("Arguments are: " + Arrays.asList(commandLine.getArgs()));
  }

  public boolean hasOption(char arg) {
    return commandLine.hasOption(arg);
  }

  public boolean hasOption(String arg) {
    return commandLine.hasOption(arg);
  }

  public String getOptionValue(char arg) {
    return commandLine.getOptionValue(arg);
  }

  public String getOptionValue(String arg) {
    return commandLine.getOptionValue(arg);
  }

  public static String readPassword() {
    try {
      System.out.print("Enter password: ");
      return new jline.ConsoleReader().readLine(Character.valueOf('*'));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static JMXConnector getJMXConnector(String host, int port) {
    return new JMXConnectorProxy(host, port);
  }

  public static JMXConnector getJMXConnector(String username, String password, String host, int port) {
    Map env = new HashMap();
    if (username != null && password != null) {
      String[] creds = { username, password };
      env.put("jmx.remote.credentials", creds);
    }
    return new JMXConnectorProxy(host, port, env);
  }
}
