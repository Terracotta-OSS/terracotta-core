/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import java.io.File;
import java.util.Arrays;

public class ReviveClassFilesTest extends AbstractDBUtilsTest {

  public void testReviveFileClass() throws Exception {

    File databaseDir = new File(getTempDirectory().toString() + File.separator + "db-data");
    File classOutputDir = new File(getTempDirectory().toString() + File.separator + "class-output");
    databaseDir.mkdirs();
    classOutputDir.mkdirs();

    ReviveClassFiles reviveClassFiles = new ReviveClassFiles(databaseDir, classOutputDir);
    SleepycatPersistor sleepycatPersistor = reviveClassFiles.getSleepycatPersistor();
    populateSleepycatDB(sleepycatPersistor);

    reviveClassFiles.reviveClassesFiles(classOutputDir);
    String[] files = classOutputDir.list();
    Arrays.sort(files);
    for (int j = 1; j < files.length; j++) {
      assert (files[j].contains("Sample" + j));
    }

  }

}
