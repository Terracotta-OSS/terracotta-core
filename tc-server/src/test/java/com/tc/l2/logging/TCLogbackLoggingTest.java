/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.l2.logging;

import static com.tc.l2.logging.TCLogbackLogging.CONSOLE;
import java.io.File;
import java.nio.file.Files;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class TCLogbackLoggingTest {

  public @Rule SystemOutRule systemOutRule = new SystemOutRule().enableLog();
  public @Rule TemporaryFolder temp = new TemporaryFolder();

  public TCLogbackLoggingTest() {
  }

  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
    TCLogbackLogging.resetLogging();
  }

  /**
   * Test of bootstrapLogging method, of class TCLogbackLogging.
   */
  @Test
  public void testBootstrapLogging() throws Exception {
    System.out.println("bootstrapLogging");
    TCLogbackLogging.resetLogging();
    TCLogbackLogging.bootstrapLogging(null);

    // test that console logger is properly installed
    Logger test = LoggerFactory.getLogger(CONSOLE);
    systemOutRule.clearLog();
    test.info("this is a test");

    // redirect logging so buffered lines reach the console and the file
    File logDir = temp.newFolder();
    systemOutRule.clearLog();
    TCLogbackLogging.redirectLogging(logDir);

    String consoleLog = systemOutRule.getLog();
    assertThat("Buffered message should hit the console once logging is redirected",
        consoleLog, containsString("this is a test"));
    assertThat("Console should announce redirection target", consoleLog, containsString("Log file:"));

    File logFile = new File(logDir, "terracotta.server.log");
    assertTrue("Log file should exist after redirect", logFile.exists());
    String fileContents = Files.readString(logFile.toPath());
    assertThat("Buffered message should be written to the log file",
        fileContents, containsString("this is a test"));
  }

  /**
   * Test of redirectLogging method, of class TCLogbackLogging.
   */
  @Test
  public void testRedirectLogging() throws Exception {
    System.out.println("bootstrapLogging");
    TCLogbackLogging.resetLogging();
    TCLogbackLogging.bootstrapLogging(null);

    // test that console logger is properly installed
    Logger test = LoggerFactory.getLogger(CONSOLE);
    systemOutRule.clearLog();
    test.info("this is a test");

    File folder = temp.newFolder();
    systemOutRule.clearLog();
    TCLogbackLogging.redirectLogging(folder);
    assertThat(systemOutRule.getLog(), containsString("this is a test"));
    LoggerFactory.getLogger(CONSOLE).info("flush1");
    LoggerFactory.getLogger(CONSOLE).info("flush2");
    LoggerFactory.getLogger(CONSOLE).info("flush3");
    LoggerFactory.getLogger(CONSOLE).info("flush4");

    File logFile = new File(folder, "terracotta.server.log");
    assertTrue("Log file should exist after redirect", logFile.exists());
    String fileContents = Files.readString(logFile.toPath());
    assertTrue(fileContents.contains("this is a test"));
  }

  @Test
  public void testResetLoggingForcesInfoLevel() {
    TCLogbackLogging.resetLogging();

    LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger console = ctx.getLogger(TCLogbackLogging.CONSOLE);

    assertThat(console.getEffectiveLevel(), is(Level.INFO));
  }

  @Test
  public void testBootstrapLoggingKeepsRootAtInfo() throws Exception {
    TCLogbackLogging.resetLogging();

    TCLogbackLogging.bootstrapLogging(null); // console appender setup
    
    // Redirect logging to a temporary folder to enable console output
    File folder = temp.newFolder();
    TCLogbackLogging.redirectLogging(folder);

    LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger root = ctx.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

    assertThat(root.getLevel(), is(Level.INFO));

    systemOutRule.clearLog();
    LoggerFactory.getLogger(TCLogbackLogging.CONSOLE).debug("debug-should-NOT-appear");
    LoggerFactory.getLogger(TCLogbackLogging.CONSOLE).info("info-should-appear");

    String log = systemOutRule.getLog();
    assertThat("DEBUG should be filtered", log, not(containsString("debug-should-NOT-appear")));
    assertThat("INFO should be logged", log, containsString("info-should-appear"));
  }
}
