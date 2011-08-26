/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.crasher;

import org.apache.commons.io.FileUtils;

import com.tc.exception.TCRuntimeException;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Crasher implements Runnable {

  private static final DateFormat dateFormat = new SimpleDateFormat("M/d/y:H:m:s,S (Z)");
  
  private final Arguments args;

  public Crasher(Arguments args) {
    this.args = args;
  }

  public void run() {
    System.out.println("instance count: " + args.getInstanceCount());
    System.out.println("classname: " + args.getClassname());
    System.out.println("crash freq: " + args.getCrashFrequency());
    System.out.println("args: " + args.getMainClassArgs());
    System.out.println("server args: " + args.getServerArgs());

    File outputDirectory = new File(args.getOutputDirectoryname());
    if (!outputDirectory.exists()) {
      System.out.println("output directory doesn't exist.  attempting to create: " + outputDirectory);
      try {
        FileUtils.forceMkdir(outputDirectory);
      } catch (IOException e) {
        throw new TCRuntimeException(e);
      }
    } else {
      System.out.println("output directory: " + outputDirectory);
    }

    System.out.println("output prefix: " + args.getOutputPrefix());

    ProcessContainer[] processes = new ProcessContainer[args.getInstanceCount()];
    for (int i = 0; i < processes.length; i++) {
      try {
        processes[i] = new ProcessContainer(new ProcessContainerConfig(i + "", dateFormat, args.getServerArgs(), args.getClassname(),
                                                                       args.getMainClassArgs(), outputDirectory, args
                                                                           .getOutputPrefix()));
      } catch (IOException e) {
        throw new TCRuntimeException(e);
      }
    }

    try {
      doStuff(processes);
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }
  }

  private void doStuff(ProcessContainer[] processes) throws IOException, InterruptedException {
    Random random = new Random();
    while (true) {
      for (int i = 0; i < processes.length; i++) {
        ProcessContainer process = processes[i];
        if (process.isStopped()) {
          System.out.println("Starting process " + i + "...");
          process.start();
        } else if (random.nextInt(100) <= (args.crashFrequency * 100)) {
          System.out.println("Stopping process " + i + "...");
          process.stop();
          System.out.println("Starting process " + i + "...");
          process.start();
        }
      }
      Thread.sleep(10 * 1000);
    }

  }

  public static class Arguments {
    private static final String CLASSNAME_ARG            = "--crasher:classname";
    private static final String INSTANCE_COUNT_ARG       = "--crasher:instance-count";
    private static final String CRASH_FREQUENCY_ARG      = "--crasher:crash-frequency";
    private static final String SERVER_ARGS_PREFIX       = "--crasher:server-arg";
    private static final String OUTPUT_DIRECTORYNAME_ARG = "--crasher:output-directory";
    private static final String OUTPUT_PREFIX_ARG        = "--crasher:output-prefix";

    private final List          args;
    private List                serverArgs;
    private int                 instanceCount;
    private String              classname;
    private double              crashFrequency;
    private String              outputDirectoryname;
    private String              outputPrefix;

    public Arguments(String[] args) {
      this.args = new LinkedList(Arrays.asList(args));
    }

    public List getServerArgs() {
      return new ArrayList(this.serverArgs);
    }

    public List getMainClassArgs() {
      return new ArrayList(args);
    }

    public double getCrashFrequency() {
      return this.crashFrequency;
    }

    public int getInstanceCount() {
      return this.instanceCount;
    }

    public String getClassname() {
      return this.classname;
    }

    public String getOutputDirectoryname() {
      return this.outputDirectoryname;
    }

    public String getOutputPrefix() {
      return this.outputPrefix;
    }

    public synchronized boolean parse() {
      try {
        this.instanceCount = parseInstanceCount();
        this.classname = parseClassname();
        this.crashFrequency = parseCrashFrequency();
        this.serverArgs = parseServerArgs();
        this.outputDirectoryname = parseOutputDirectoryname();
        this.outputPrefix = parseOutputPrefix();
        addClasspath();

      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
      return true;
    }

    private void addClasspath() {
      this.serverArgs.add("-classpath");
      this.serverArgs.add(System.getProperty("java.class.path"));
    }

    private String parseOutputPrefix() {
      String rv = getArgumentValueAndPruneArgsArray(OUTPUT_PREFIX_ARG);
      if (rv == null) { throw new RuntimeException("No such argument: " + OUTPUT_PREFIX_ARG); }
      return rv;
    }

    private String parseOutputDirectoryname() {
      String rv = getArgumentValueAndPruneArgsArray(OUTPUT_DIRECTORYNAME_ARG);
      if (rv == null) { throw new RuntimeException("No such argument: " + OUTPUT_DIRECTORYNAME_ARG); }
      return rv;
    }

    private List parseServerArgs() {
      return getArgumentValuesAndPruneArgsArray(new ArrayList(), SERVER_ARGS_PREFIX);
    }

    private int parseInstanceCount() throws Exception {
      return Integer.parseInt(getArgumentValueAndPruneArgsArray(INSTANCE_COUNT_ARG));
    }

    private String parseClassname() throws Exception {
      String rv = getArgumentValueAndPruneArgsArray(CLASSNAME_ARG);
      if (rv == null) { throw new RuntimeException("No such argument: " + CLASSNAME_ARG); }
      return rv;
    }

    private double parseCrashFrequency() throws Exception {
      return Double.parseDouble(getArgumentValueAndPruneArgsArray(CRASH_FREQUENCY_ARG));
    }

    private String getArgumentValueAndPruneArgsArray(String argumentPattern) {
      List values = getArgumentValuesAndPruneArgsArray(new ArrayList(), argumentPattern);
      return (String) ((values.size() > 0) ? values.get(0) : null);
    }

    private List getArgumentValuesAndPruneArgsArray(List list, String argumentPattern) {
      List tmp = getArgumentsAndPruneArgsArray(new ArrayList(), argumentPattern);
      for (Iterator i = tmp.iterator(); i.hasNext();) {
        String arg = (String) i.next();
        list.add(arg.substring(arg.indexOf('=') + 1, arg.length()));
      }
      return list;
    }

    private List getArgumentsAndPruneArgsArray(List list, String argumentPattern) {
      for (Iterator i = args.iterator(); i.hasNext();) {
        String arg = (String) i.next();
        if (arg.startsWith(argumentPattern)) {
          i.remove();
          list.add(arg);
        }
      }
      return list;
    }

    public StringBuffer usage(StringBuffer buf) {
      buf.append("Crasher -- starts, crashes, and restarts Container instances.\n");
      buf.append("Usage:\n");
      buf.append("\tjava " + Crasher.class.getName() + " " + INSTANCE_COUNT_ARG + "=<instance count> "
                 + CRASH_FREQUENCY_ARG + "=<crash frequency [0-1]> " + "[[" + SERVER_ARGS_PREFIX
                 + "=<server arg>],...] " + OUTPUT_DIRECTORYNAME_ARG + "=<output directory> " + OUTPUT_PREFIX_ARG
                 + "=<output prefix> " + CLASSNAME_ARG + "=<main class> <main class args>\n\n");
      return buf;
    }

  }

  public static void main(String[] args) throws Exception {
    Arguments arguments = new Arguments(args);
    if (!arguments.parse()) {
      System.out.println(arguments.usage(new StringBuffer()));
      return;
    }
    new Crasher(arguments).run();
  }

}