/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.storage.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.tc.objectserver.managedobject.bytecode.PhysicalStateClassLoader;
import com.tc.objectserver.persistence.db.AbstractDBUtilsTestBase;
import com.tc.objectserver.persistence.db.DBPersistorImpl;
import com.tc.objectserver.storage.util.ReviveClassFiles;
import com.tc.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

public class ReviveClassFilesTest extends AbstractDBUtilsTestBase {

  public void testReviveFileClass() throws Exception {

    File databaseDir = new File(getTempDirectory().toString() + File.separator + "db-data");
    File classOutputDir = new File(getTempDirectory().toString() + File.separator + "class-output");
    databaseDir.mkdirs();
    classOutputDir.mkdirs();

    ReviveClassFiles reviveClassFiles = new ReviveClassFiles(databaseDir, classOutputDir);
    DBPersistorImpl sleepycatPersistor = reviveClassFiles.getPersistor(1);
    populateSleepycatDB(sleepycatPersistor);

    reviveClassFiles.reviveClassesFiles();
    String[] files = classOutputDir.list();
    Arrays.sort(files);
    for (int j = 0; j < files.length; j++) {
      Assert.assertTrue(files[j].contains("idx" + (j + 1)));
    }

    PhysicalStateClassLoader loader = new PhysicalStateClassLoader();

    File[] filesList = classOutputDir.listFiles();
    for (int i = 0; i < filesList.length; i++) {
      File file = filesList[i];
      FileInputStream fis = new FileInputStream(file);
      byte[] b = IOUtils.toByteArray(fis);
      System.out.println("file: " + file.getName());
      String genClassName = StringUtils.substringBefore(file.getName(), ".class");
      System.out.println("genClassName: " + genClassName);
      try {
        Class clazz = loader.defineClassFromBytes(genClassName, 0, b, 0, b.length);
        if(clazz == null) {
          fail("could not load class");
        }
      } catch (Exception e) {
        fail("exception loading class");
      }
    }

  }
}
