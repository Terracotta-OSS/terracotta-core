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
import java.io.FileReader;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.util.Arrays;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class TCLogbackLoggingTest {

  public @Rule SystemOutRule sysout = new SystemOutRule().enableLog();
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
    test.info("this is a test");
    assertThat(sysout.getLog(), not(containsString("this is a test")));
    File f = temp.newFolder();
    TCLogbackLogging.redirectLogging(f);
    assertThat(sysout.getLog(), containsString("this is a test"));
    sysout.clearLog();
    assertThat(sysout.getLog(), not(containsString("this is a test")));
    System.out.println("PRINTING " + f.listFiles()[0].toPath());
    Files.readAllLines(f.listFiles()[0].toPath()).forEach(System.out::println);
    System.out.println("FINISHED " + f.listFiles()[0].toPath());
    assertThat(Files.readAllLines(f.listFiles()[0].toPath()), contains(Arrays.asList(containsString("this is a test"), containsString("Log file:"))));
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
    test.info("this is a test");
    assertThat(sysout.getLog(), not(containsString("this is a test")));

    File folder = temp.newFolder();
    TCLogbackLogging.redirectLogging(folder);
    assertThat(sysout.getLog(), containsString("this is a test"));
    LoggerFactory.getLogger(CONSOLE).info("flush1");
    LoggerFactory.getLogger(CONSOLE).info("flush2");
    LoggerFactory.getLogger(CONSOLE).info("flush3");
    LoggerFactory.getLogger(CONSOLE).info("flush4");

    FileReader read = new FileReader(new File(folder, "terracotta.server.log"));
    LineNumberReader lines = new LineNumberReader(read);
    boolean contains = false;
    String line = lines.readLine();
    while (line != null) {
      System.out.println("TESTING " + line);
      if (line.contains("this is a test")) {
        contains = true;
        break;
      }
      line = lines.readLine();
    }
    assertTrue(contains);
  }

}
