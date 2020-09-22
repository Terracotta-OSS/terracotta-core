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

import com.tc.logging.TCLogging;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
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
  public void testBootstrapLogging() {
    System.out.println("bootstrapLogging");
    TCLogbackLogging.resetLogging();
    TCLogbackLogging.bootstrapLogging();

    // test that console logger is properly installed
    Logger test = TCLogging.getConsoleLogger();
    test.info("this is a test");
    assertThat(sysout.getLog(), containsString("this is a test"));
    sysout.clearLog();
    assertThat(sysout.getLog(), not(containsString("this is a test")));
    TCLogbackLogging.redirectLogging(null);
    assertThat(sysout.getLog(), containsString("this is a test"));
  }

  /**
   * Test of redirectLogging method, of class TCLogbackLogging.
   */
  @Test
  public void testRedirectLogging() throws Exception {
    System.out.println("bootstrapLogging");
    TCLogbackLogging.resetLogging();
    TCLogbackLogging.bootstrapLogging();

    // test that console logger is properly installed
    Logger test = TCLogging.getConsoleLogger();
    test.info("this is a test");
    assertThat(sysout.getLog(), containsString("this is a test"));

    File folder = temp.newFolder();
    TCLogbackLogging.redirectLogging(folder);
    TCLogging.getConsoleLogger().info("flush1");
    TCLogging.getConsoleLogger().info("flush2");
    TCLogging.getConsoleLogger().info("flush3");
    TCLogging.getConsoleLogger().info("flush4");

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
