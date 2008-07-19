/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.tc.test.TCTestCase;

import java.io.File;
import java.io.IOException;

public class SleepycatCollectionsTest extends TCTestCase {

  private DBEnvironment env;
  private static int    dbHomeCounter = 0;
  private static File   tempDirectory;

  public SleepycatCollectionsTest() {
    // MNK-649
    disableAllUntil("2008-08-15");
  }
  
  @Override
  public void setUp() throws Exception {

    if (env != null) env.close();
    File dbHome = newDBHome();
    env = new DBEnvironment(true, dbHome);
  }

  // XXX:: Check SleepycatSerializationTest if you want know why its done like this or ask Orion.
  private File newDBHome() throws IOException {
    File file;
    if (tempDirectory == null) tempDirectory = getTempDirectory();
    ++dbHomeCounter;
    for (file = new File(tempDirectory, "db" + dbHomeCounter); file.exists(); ++dbHomeCounter) {
      //
    }
    assertFalse(file.exists());
    System.err.println("DB Home = " + file);
    return file;
  }

  @Override
  public void tearDown() throws Exception {
    // persistor = null;
    // ptp = null;
    env = null;
  }
}
