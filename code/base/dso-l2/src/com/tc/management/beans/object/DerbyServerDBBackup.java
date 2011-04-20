/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans.object;

import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.stats.AbstractNotifyingMBean;

import javax.management.NotCompliantMBeanException;

/**
 * This is just a null implementation. TODO: implement a proper server db backup later.
 */
public class DerbyServerDBBackup extends AbstractNotifyingMBean implements ServerDBBackupMBean {

  public DerbyServerDBBackup(String destDir) throws NotCompliantMBeanException {
    super(ServerDBBackupMBean.class);
  }

  public DerbyServerDBBackup(L2ConfigurationSetupManager configSetupManager) throws NotCompliantMBeanException {
    super(ServerDBBackupMBean.class);
  }

  public void reset() {
    //
  }

  public boolean isBackUpRunning() {
    return false;
  }

  public String getDefaultPathForBackup() {
    return null;
  }

  public void runBackUp() {
    //
  }

  public void runBackUp(String path) {
    //
  }

  public boolean isBackupEnabled() {
    return true;
  }

  public String getDbHome() {
    return null;
  }

}
