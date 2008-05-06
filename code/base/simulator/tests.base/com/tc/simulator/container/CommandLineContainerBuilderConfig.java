/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.container;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.tcsimulator.ContainerBuilder;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;

class CommandLineContainerBuilderConfig implements ContainerBuilderConfig {
  public static final String INTENSITY_OPTION                     = "intensity";
  public static final String APP_EXEC_COUNT_OPTION                = "app-exec-count";
  public static final String GLOBAL_PARTICIPANT_COUNT_OPTION      = "global-participant-count";
  public static final String GLOBAL_CONTAINER_COUNT_OPTION        = "global-container-count";
  public static final String OUTPUT_OPTION                        = "output";
  public static final String CONSOLE_OUTPUT_VALUE                 = "console";
  public static final String APP_CONFIG_BUILDER_OPTION            = "app-config-builder";
  public static final String MASTER_OPTION                        = "master";
  public static final String START_SERVER_OPTION                  = "start-server";
  public static final String HELP_OPTION                          = "help";
  public static final String CONTAINER_START_TIMEOUT_OPTION       = "container-start-timeout";
  public static final String APPLICATION_START_TIMEOUT_OPTION     = "app-start-timeout";
  public static final String APPLICATION_EXECUTION_TIMEOUT_OPTION = "app-exec-timeout";
  public static final String DUMP_ERRORS_OPTION                   = "dump-errors";
  public static final String DUMP_OUTPUT_OPTION                   = "dump-output";
  public static final String AGGREGATE_OPTION                     = "aggregate";
  public static final String STREAM_OPTION                        = "stream";
  public static final String APPLICATION_CLASSNAME_OPTION         = "application-classname";

  private final String[]     args;
  private final Options      options;
  private CommandLine        commandLine;
  private Option             help;
  private Option             startServer;
  private Option             master;
  private Option             appConfigBuilder;
  private Option             output;
  private Option             globalParticipantCount;
  private Option             applicationExecutionCount;
  private Option             containerStartTimeout;
  private Option             applicationStartTimeout;
  private Option             applicationExecutionTimeout;
  private Option             dumpErrors;
  private Option             dumpOutput;
  private Option             aggregate;
  private Option             stream;
  private Option             intensity;
  private Option             globalContainerCount;
  private Option             applicationClassname;

  CommandLineContainerBuilderConfig(String[] args) {
    this.args = args;
    this.options = newOptions();
  }

  public String toString() {
    StringBuffer rv = new StringBuffer();

    for (Iterator iter = Arrays.asList(commandLine.getOptions()).iterator(); iter.hasNext();) {
      Option opt = (Option) iter.next();
      rv.append(opt + "=");
      String[] values = opt.getValues();
      for (int i = 0; values != null && i < values.length; i++) {
        rv.append(values[i]);
        if (i < values.length - 1) {
          rv.append(",");
        }
      }
      rv.append("\n");
    }
    return rv.toString();
  }

  boolean help() {
    return asBoolean(this.help);
  }

  public boolean startServer() {
    return asBoolean(this.startServer);
  }

  public boolean master() {
    return asBoolean(this.master);
  }

  public String appConfigBuilder() {
    return asString(null, this.appConfigBuilder);
  }

  public boolean outputToConsole() {
    return CONSOLE_OUTPUT_VALUE.equals(asString(CONSOLE_OUTPUT_VALUE, this.output));
  }

  public boolean outputToFile() {
    return !outputToConsole();
  }

  public int globalParticipantCount() {
    return asInt(-1, this.globalParticipantCount);
  }

  public File outputFile() {
    return new File(asString(CONSOLE_OUTPUT_VALUE, this.output));
  }

  public int getApplicationExecutionCount() {
    return asInt(1, this.applicationExecutionCount);
  }

  public long getContainerStartTimeout() {
    return asLong(5 * 60 * 1000, this.containerStartTimeout);
  }

  public long getApplicationStartTimeout() {
    return asLong(5 * 60 * 1000, this.applicationStartTimeout);
  }

  public long getApplicationExecutionTimeout() {
    return asLong(30 * 60 * 1000, this.applicationExecutionTimeout);
  }

  public boolean dumpErrors() {
    return asBoolean(this.dumpErrors);
  }

  public boolean dumpOutput() {
    return asBoolean(this.dumpOutput);
  }

  public boolean aggregate() {
    return asBoolean(this.aggregate);
  }

  public boolean stream() {
    return asBoolean(this.stream);
  }

  public int globalContainerCount() {
    return asInt(0, this.globalContainerCount);
  }

  public int intensity() {
    return asInt(1, this.intensity);
  }

  private Options newOptions() {
    OptionMaker maker = new OptionMaker();
    Options rv = new Options();
    this.help = maker.withLongOpt(HELP_OPTION).withDescription("Prints this message.").create('h');
    rv.addOption(this.help);

    this.startServer = maker.withLongOpt(START_SERVER_OPTION).withDescription("Start the DSO server.").create('a');
    rv.addOption(this.startServer);

    this.master = maker.withLongOpt(MASTER_OPTION).withDescription("Designate this instance as the master.")
        .create('b');
    rv.addOption(this.master);

    this.appConfigBuilder = maker.withLongOpt(APP_CONFIG_BUILDER_OPTION)
        .withDescription("The classname of the application configuration builder (required)").withValueSeparator()
        .hasArg().isRequired().create('c');
    rv.addOption(this.appConfigBuilder);

    this.output = maker.withLongOpt(OUTPUT_OPTION).withDescription("Output sink. ('console' | <filename>)")
        .withValueSeparator().hasArg().create('d');
    rv.addOption(this.output);

    this.globalParticipantCount = maker.withLongOpt(GLOBAL_PARTICIPANT_COUNT_OPTION)
        .withDescription("Total number of application instances cluster-wide").hasArg().isRequired().create('e');
    rv.addOption(this.globalParticipantCount);

    this.applicationExecutionCount = maker.withLongOpt(APP_EXEC_COUNT_OPTION)
        .withDescription("Number of application instances to start in this container.").hasArg().create('f');
    rv.addOption(this.applicationExecutionCount);

    this.containerStartTimeout = maker.withLongOpt(CONTAINER_START_TIMEOUT_OPTION)
        .withDescription("Timeout for all containers to start.").hasArg().create('g');
    rv.addOption(this.containerStartTimeout);

    this.applicationStartTimeout = maker.withLongOpt(APPLICATION_START_TIMEOUT_OPTION)
        .withDescription("Timeout for all application instances within a given container to start.").hasArg()
        .create('h');
    rv.addOption(this.applicationStartTimeout);

    this.applicationExecutionTimeout = maker.withLongOpt(APPLICATION_EXECUTION_TIMEOUT_OPTION)
        .withDescription("Timeout for all application instances within a given container to execute.").hasArg()
        .create('i');
    rv.addOption(this.applicationExecutionTimeout);

    this.dumpErrors = maker.withLongOpt(DUMP_ERRORS_OPTION).withDescription("Dump errors to System.err as they occur.")
        .create('j');
    rv.addOption(this.dumpErrors);

    this.dumpOutput = maker.withLongOpt(DUMP_OUTPUT_OPTION).withDescription("Dump results as they are reported.")
        .create('k');
    rv.addOption(this.dumpOutput);

    this.aggregate = maker.withLongOpt(AGGREGATE_OPTION)
        .withDescription("Aggregate output for batch send when run is complete.").create('l');
    rv.addOption(this.aggregate);

    this.stream = maker.withLongOpt(STREAM_OPTION).withDescription("Stream output immediately.").create('m');
    rv.addOption(stream);

    this.intensity = maker.withLongOpt(INTENSITY_OPTION).withDescription("Intensity of test.").hasArg().create('n');
    rv.addOption(intensity);

    this.globalContainerCount = maker.withLongOpt(GLOBAL_CONTAINER_COUNT_OPTION)
        .withDescription("The total number of Containers int this test.").hasArg().create('o');
    rv.addOption(this.globalContainerCount);

    this.applicationClassname = maker.withLongOpt(APPLICATION_CLASSNAME_OPTION)
        .withDescription("Name of the test application.").withValueSeparator().hasArg().create('p');
    rv.addOption(this.applicationClassname);

    return rv;
  }

  private boolean asBoolean(Option opt) {
    return commandLine.hasOption(opt.getLongOpt()) || commandLine.hasOption(opt.getOpt());
  }

  private int asInt(int defaultValue, Option opt) {
    String value = asString(null, opt);
    return (value == null) ? defaultValue : Integer.parseInt(value);
  }

  private long asLong(long defaultValue, Option opt) {
    String value = asString(null, opt);
    return (value == null) ? defaultValue : Long.parseLong(value);
  }

  private String asString(String defaultValue, Option opt) {
    String value;
    // value = (opt.hasLongOpt()) ? commandLine.getOptionValue(opt.getLongOpt()) : commandLine
    // .getOptionValue(opt.getOpt());
    value = commandLine.getOptionValue(opt.getOpt());
    return (value == null) ? defaultValue : value;
  }

  void parse() throws ParseException {
    this.commandLine = new PosixParser().parse(options, args);
  }

  void usage() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("java <options> " + ContainerBuilder.class.getName(), options);
  }

  public String applicationClassname() {
    return asString(null, this.applicationClassname);
  }
}