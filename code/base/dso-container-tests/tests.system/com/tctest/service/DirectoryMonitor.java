/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DirectoryMonitor implements DirectoryMonitorMBean {
  private long       scanRate  = 1000L;
  private String     directory = ".";
  private Thread     scanThread;
  private boolean    started;
  private String     fileExts;

  private List       list      = new ArrayList();

  public void setScanRate(long rate) {
    scanRate = rate;
  }

  public long getScanRate() {
    return scanRate;
  }

  public void setDirectory(String dir) {
    directory = dir;
  }

  public String getDirectory() {
    return directory;
  }

  public void setExtensionList(String list) {
    this.fileExts = list;
  }

  public String getExtensionList() {
    return this.fileExts;
  }

  public void start() {
    if (started) return;
    
    synchronized (list) {
      list.add(new Integer(list.hashCode()));
      list.notifyAll();
    }
    
    started = true;
    scanThread = new Thread(new ScannerThread(this));
    scanThread.start();
    System.out.println("**** Directory Monitor - Started with scan rate " + this.getScanRate() + " *****");
  }

  public boolean isStarted() {
    return started;
  }

  public void stop() {
    if (!started) return;

    synchronized (this) {
      started = false;
    }
    System.out.println("Directory Monitor - requesting stop.");
  }

  public void takeAction(Collection files) {
    System.out.println("********* Scanner Detected " + files.size() + " Files ********* ");
  }

  private class ScannerThread extends Thread {
    private DirectoryMonitor monitor;

    public ScannerThread(DirectoryMonitor m) {
      monitor = m;
    }

    public void run() {
      File dir = null;
      String[] exts = null;

      dir = new File(monitor.getDirectory());
      if (!dir.isDirectory()) {
        System.err.println(" Specified directory is invalid.");
      }

      if (monitor.getExtensionList() != null) {
        exts = monitor.getExtensionList().equals("*") ? null : monitor.getExtensionList().split(",");
      }
      // scan for files - log when files are found.
      for (;;) {
        try {
          System.out.println("Scanning ... ");          
          Collection files = Arrays.asList(dir.listFiles());

          // if files are found, call Monitor.act();
          if (files.size() > 0) {
            monitor.takeAction(files);
          } else {
            System.out.println("... found nothing");
          }

          sleep(monitor.getScanRate());
        } catch (InterruptedException e) {
          System.err.println(" Error while scanning - " + e);
        }

        if (!monitor.isStarted()) {
          System.out.println("Stopping Directory Monitor.");
          break;
        }
      }
    }
  }
}
