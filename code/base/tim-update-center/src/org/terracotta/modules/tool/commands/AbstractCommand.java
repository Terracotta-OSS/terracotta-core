/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convenience base class for commands that offers common facilities used by many command implementations.
 * 
 * @author Jason Voegele (jvoegele@terracotta.org)
 */
public abstract class AbstractCommand implements Command {
  private static final String   OPTION_HELP   = "h";
  private static final String   LONGOPT_HELP  = "help";
  private static final String   OPTION_PROXY  = "p";
  private static final String   LONGOPT_PROXY = "proxy";

  protected Options             options       = createOptions();
  protected Map<String, String> arguments     = createArguments();

  protected PrintWriter         out           = new PrintWriter(System.out, true);
  protected PrintWriter         err           = new PrintWriter(System.err, true);

  protected final Options createOptions() {
    Options opts = new Options();
    opts.addOption(OPTION_HELP, LONGOPT_HELP, false, "Display help information");
    opts.addOption(OPTION_PROXY, LONGOPT_PROXY, true, "HTTP proxy to use for remote operations");
    return opts;
  }

  protected final Map<String, String> createArguments() {
    Map<String, String> args = new HashMap<String, String>();
    return args;
  }

  public String help() {
    StringWriter writer = new StringWriter();
    PrintWriter print = new PrintWriter(writer);
    print.println(description());
    print.println();
    print.println("usage:");
    print.println("  " + name() + " " + syntax());
    print.println();
    if (!arguments.isEmpty()) {
      print.println("arguments:");
      for (Entry<String, String> arg : arguments.entrySet()) {
        print.println("  " + StringUtils.rightPad(arg.getKey(), 22) + arg.getValue());
      }
      print.println();
    }
    if (!options.getOptions().isEmpty()) {
      print.println("options:");
      List<Option> list = new ArrayList<Option>(options.getOptions());
      for (Option opt : list) {
        String shortOpt = (opt.getOpt() == null) ? "" : "-" + opt.getOpt();
        String longOpt = (opt.getLongOpt() == null) ? "" : "--" + opt.getLongOpt();
        if ((shortOpt.length() != 0) && (longOpt.length() != 0)) shortOpt += ", ";
        longOpt = StringUtils.leftPad(shortOpt, 6) + longOpt;
        print.print(StringUtils.rightPad(longOpt, 20) + "\t" + opt.getDescription());
        print.println();
      }
    }
    return StringUtils.chomp(writer.toString());
  }

  // private static final String EMPTY_HELP_STRING = "";
  //
  // protected String loadHelp() {
  // return loadHelp(getClass().getSimpleName());
  // }

  public void printHelp() {
    out.println(help());
  }

  private static final Pattern classNamePattern = Pattern.compile("([A-Za-z0-9_]+)Command");

  public String syntax() {
    return name() + " [arguments]";
  }

  public String description() {
    return "No description.";
  }

  /**
   * Default implementation that returns the name of the class (in lowercase) minus the "Command" suffix if it has one,
   */
  public String name() {
    String commandName = getClass().getSimpleName();
    Matcher matcher = classNamePattern.matcher(commandName);
    if (matcher.matches()) {
      commandName = matcher.group(1);
    }
    return commandName.toLowerCase();
  }

  public Options options() {
    return options;
  }

  // protected String loadHelp(String topic) {
  // String resourceName = "/" + getClass().getPackage().getName().replace('.', '/') + "/" + topic + ".help";
  // InputStream in = AbstractCommand.class.getResourceAsStream(resourceName);
  // try {
  // if (in == null) return EMPTY_HELP_STRING;
  // List<String> lines = IOUtils.readLines(in);
  // StringBuffer buffer = new StringBuffer();
  // for (String line : lines) {
  // buffer.append(line);
  // buffer.append(System.getProperty("line.separator"));
  // }
  // return buffer.toString();
  // } catch (IOException e) {
  // this.err.println("Unable to load resource: " + resourceName);
  // return EMPTY_HELP_STRING;
  // }
  // }

  static Option buildOption(String optname, String description) {
    OptionBuilder.withLongOpt(optname);
    OptionBuilder.withDescription(description);
    return OptionBuilder.create();
  }

  static Option buildOption(String optname, String description, Object type) {
    OptionBuilder.hasArg();
    OptionBuilder.withArgName(optname);
    OptionBuilder.withType(type);
    OptionBuilder.withLongOpt(optname);
    OptionBuilder.withDescription(description);
    return OptionBuilder.create();
  }
}
