/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import org.apache.commons.io.FileUtils;

import com.google.common.io.Files;
import com.tc.lcp.LinkedJavaProcess;
import com.tc.process.Exec;
import com.tc.process.Exec.Result;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TCLoggingTest extends TestCase {

  public static class LogWorker {
    public static void main(String[] args) {
      System.out.println("did logging");
      TCLogging.setLogDirectory(new File(args[0]), TCLogging.PROCESS_TYPE_GENERIC);
      TCLogger logger = TCLogging.getTestingLogger(LogWorker.class);
      logger.info("Hakunah Matata");
    }
  }


  public void testRollover() {
    String logDir = "/tmp/terracotta/test/com/tc/logging";
    File logDirFolder = new File(logDir);
    logDirFolder.mkdirs();

    try {
      FileUtils.cleanDirectory(logDirFolder);
    } catch (IOException e) {
      System.out.println("Unable to clean the temp log directory !! Exiting...");
      System.exit(-1);
    }

    final int LOG_ITERATIONS = 5;
    for (int i = 0; i < LOG_ITERATIONS; i++) {
      createLogs(logDir);
    }

    File[] listFiles = logDirFolder.listFiles();
    int logFileCount = 0;
    for(File file:listFiles){
      if (!file.isHidden()) {
        logFileCount++;
        Assert.assertEquals(Files.getFileExtension(file.getName()), "log");
      }
    }
    // Always one extra file is created by log4j
    Assert.assertEquals(LOG_ITERATIONS + 1, logFileCount);
  }

  private void createLogs(String logDir) {
    List<String> params = new ArrayList<String>();
    params.add(logDir);
    LinkedJavaProcess logWorkerProcess = new LinkedJavaProcess(LogWorker.class.getName(), params, null);
    try {
      logWorkerProcess.start();
      Result result = Exec.execute(logWorkerProcess, logWorkerProcess.getCommand(), null, null, null);
      if (result.getExitCode() != 0) { throw new AssertionError("LogWorker Exit code is " + result.getExitCode()); }

    } catch (Exception e) {
      System.out.println("Unable to log. Exiting...");
      System.exit(-1);
    }
  }
}