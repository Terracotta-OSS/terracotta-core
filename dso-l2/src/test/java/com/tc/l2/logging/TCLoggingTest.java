/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.l2.logging;

import com.tc.lcp.LinkedJavaProcess;
import org.apache.commons.io.FileUtils;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.process.Exec;
import com.tc.process.Exec.Result;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.junit.Test;


public class TCLoggingTest extends TCTestCase {

  public static class LogWorker {
    public static void main(String[] args) {
      System.out.println("did logging");
      TCLogging.setLogLocationAndType(new File(args[0]).toURI());
      TCLogger logger = TCLogging.getLogger(LogWorker.class);
      logger.info("Data for Logs");
    }
  }

  public void testRollover() throws Exception {
    String logDir = Files.createTempDirectory("tc-logging").toFile().getAbsolutePath();
    File logDirFolder = new File(logDir);
    logDirFolder.mkdirs();

    try {
      FileUtils.cleanDirectory(logDirFolder);
    } catch (IOException e) {
      Assert.fail("Unable to clean the temp log directory !! Exiting...");
    }

    final int LOG_ITERATIONS = 5;
    for (int i = 0; i < LOG_ITERATIONS; i++) {
      createLogs(logDir);
    }

    File[] listFiles = logDirFolder.listFiles();
    int logFileCount = 0;
    for (File file : listFiles) {
      String ext = file.getName().substring(file.getName().lastIndexOf('.') + 1);
      if (!file.isHidden() && ext.equals("log")) {
        logFileCount++;
      }
    }
    // Always one extra file is created by log4j
    Assert.assertEquals(LOG_ITERATIONS + 1, logFileCount);

  }
  
  @Test
  public void testDeveloperOverlay() throws Exception {
    Properties classpath = new Properties();
    Properties userhome = new Properties();
    Properties userdir = new Properties();
    
    classpath.setProperty("whoami", "classpath");
    userhome.setProperty("whoami", "userhome");
    userdir.setProperty("whoami", "userdir");
    
    ByteArrayOutputStream classbytes = new ByteArrayOutputStream();
    ByteArrayOutputStream homebytes = new ByteArrayOutputStream();
    ByteArrayOutputStream dirbytes = new ByteArrayOutputStream();
    
    classpath.store(classbytes, null);
    userhome.store(homebytes, null);
    userdir.store(dirbytes, null);
    
    Assert.assertEquals(((TCLoggingLog4J)TCLogging.getLoggingService()).layerDevelopmentConfiguration(Arrays.asList(
        new ByteArrayInputStream(classbytes.toByteArray()),
        new ByteArrayInputStream(homebytes.toByteArray()),
        new ByteArrayInputStream(dirbytes.toByteArray()))).getProperty("whoami"), "userdir");
// make sure empty streams return a valid props file
    Assert.assertNotNull(((TCLoggingLog4J)TCLogging.getLoggingService()).layerDevelopmentConfiguration(Arrays.asList(
        new ByteArrayInputStream(new byte[0]),
        new ByteArrayInputStream(new byte[0]),
        new ByteArrayInputStream(new byte[0]))));    
// make sure an empty list returns null
    Assert.assertNull(((TCLoggingLog4J)TCLogging.getLoggingService()).layerDevelopmentConfiguration(Collections.<InputStream>emptyList()));
  }

  private void createLogs(String logDir) throws Exception {
    List<String> params = new ArrayList<String>();
    params.add(logDir);
    LinkedJavaProcess logWorkerProcess = new LinkedJavaProcess(LogWorker.class.getName(), params, null);
    logWorkerProcess.setDirectory(getTempDirectory());
    try {
      logWorkerProcess.start();
      Result result = Exec.execute(logWorkerProcess, logWorkerProcess.getCommand(), null, null, getTempDirectory());
      if (result.getExitCode() != 0) { throw new AssertionError("LogWorker Exit code is " + result.getExitCode()); }

    } catch (Exception e) {
      Assert.fail("Unable to log. Exiting...");
    }
  }
}
