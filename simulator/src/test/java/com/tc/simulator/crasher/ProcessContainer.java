/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.crasher;

import org.apache.commons.io.FileUtils;

import com.tc.process.StreamCopier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProcessContainer {

  private static int                   STOPPED = 1;
  private static int                   RUNNING = 3;

  private final ProcessContainerConfig config;
  private final List                   command = new ArrayList();
  private final PrintStream            out;
  private final DateFormat             dateFormat;

  private int                          state   = STOPPED;
  private Process                      process;
  private Thread                       outcopy;
  private Thread                       errcopy;

  public ProcessContainer(ProcessContainerConfig config) throws IOException {
    this.config = config;
    this.dateFormat = config.getDateFormat();
    createCommand();
    System.out.println("command: " + command);
    File outfile = new File(config.getOutputDirectory(), config.getOutputPrefix() + "." + config.getID() + ".out");
    FileUtils.forceMkdir(outfile.getParentFile());
    FileUtils.touch(outfile);
    out = new PrintStream(new FileOutputStream(outfile));
  }

  public synchronized boolean isStopped() {
    return state == STOPPED;
  }

  public synchronized void start() throws IOException {
    if (state != STOPPED) return;
    println("starting process...");
    this.process = Runtime.getRuntime().exec((String[]) command.toArray(new String[command.size()]));
    this.outcopy = new Thread(new StreamCopier(this.process.getInputStream(), out));
    this.errcopy = new Thread(new StreamCopier(this.process.getErrorStream(), out));
    this.outcopy.start();
    this.errcopy.start();
    state = RUNNING;
  }

  public synchronized void stop() {
    if (state != RUNNING) return;
    println("stopping process...");
    this.process.destroy();
    synchronized (outcopy) {
      outcopy.interrupt();
    }
    synchronized (errcopy) {
      errcopy.interrupt();
    }
    state = STOPPED;
  }

  private void println(Object o) {
    out.println(prefix() + ":" + o);
  }

  private String prefix() {
    return dateFormat.format(new Date()) + ": " + Thread.currentThread() + ": " + config.getID();
  }

  private void createCommand() {
    command.add(getJavaCommand());
    command.addAll(config.getServerArgs());
    command.add(config.getClassname());
    command.addAll(config.getMainClassArgs());
  }

  private String getJavaCommand() {
    return "java";
  }
}