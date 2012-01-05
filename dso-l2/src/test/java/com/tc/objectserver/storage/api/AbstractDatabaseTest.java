/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import org.apache.commons.io.FileUtils;

import com.tc.test.TCTestCase;

import java.io.File;

public abstract class AbstractDatabaseTest extends TCTestCase {
  private DBEnvironment                  dbenv;
  private PersistenceTransactionProvider ptp;

  @Override
  protected void setUp() throws Exception {
    File dbHome = new File(getTempDirectory(), getName());
    if (dbHome.exists()) {
      FileUtils.cleanDirectory(dbHome);
    }
    dbHome.mkdir();
    dbenv = newDBEnvironment(dbHome);
    dbenv.open();
    ptp = newPersistenceTransactionProvider(dbenv);
  }

  protected boolean isParanoid() {
    return true;
  }

  protected DBEnvironment newDBEnvironment(File dbHome) throws Exception {
    dbenv = DBFactory.getInstance().createEnvironment(isParanoid(), dbHome);
    return dbenv;
  }

  protected PersistenceTransactionProvider newPersistenceTransactionProvider(DBEnvironment env) {
    return env.getPersistenceTransactionProvider();
  }

  protected PersistenceTransactionProvider getPtp() {
    return ptp;
  }

  protected PersistenceTransaction newTransaction() {
    return ptp.newTransaction();
  }

  protected DBEnvironment getDbenv() {
    return dbenv;
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      dbenv.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  protected boolean cleanTempDir() {
    return false;
  }
}
