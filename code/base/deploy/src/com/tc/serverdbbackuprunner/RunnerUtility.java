/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.serverdbbackuprunner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

import com.tc.management.JMXConnectorProxy;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;

import javax.management.remote.JMXConnector;

public class RunnerUtility {
  private Options     options = new Options();
  private String      callingClassName;
  private String[]    cmdArguments;
  private CommandLine commandLine;

  public RunnerUtility(String callingClassName, String[] cmdArguments) {
    this.callingClassName = callingClassName;
    this.cmdArguments = Arrays.asList(cmdArguments).toArray(new String[0]);
  }
  
  public void setOptions(Options options) {
    this.options = options;
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
    new HelpFormatter().printHelp("java " + callingClassName, options);
    System.exit(1);
  }
  
  public void printArguments() {
    System.err.println("Arguments are: " + Arrays.asList(commandLine.getArgs()));
  }
  
  public boolean hasOption(char arg) {
    return commandLine.hasOption(arg);
  }
  
  public String getOptionValue(char arg) {
    return commandLine.getOptionValue(arg);
  }
  
  public static String readPassword() {
    try {
      Method m = System.class.getMethod("console", new Class[] {});
      Object console = m.invoke(null, (Object[]) null);
      if (console != null) {
        m = console.getClass().getMethod("readPassword", new Class[] { String.class, Object[].class });
        if (m != null) {
          byte[] pw = (byte[]) m.invoke(console, new Object[] { "[%s]", "[console] Enter Password: " });
          return new String(pw);
        }
      }
    } catch (RuntimeException re) {/**/
    } catch (Exception e) {/**/
    }
    try {
      System.out.print("Enter password: ");
      return new jline.ConsoleReader().readLine(Character.valueOf('*'));
    } catch (Exception e) {/**/
    }
    return null;
  }
  
  public static JMXConnector getJMXConnector(String userName, String host, int port) {
    HashMap env = null;
    if (userName != null) {
      env = new HashMap();
      String[] creds = { userName, readPassword() };
      env.put("jmx.remote.credentials", creds);
    }
    return new JMXConnectorProxy(host, port, env);
  }
}
