/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.installer.action;

import com.tc.installer.util.WarFileFilter;
import com.zerog.ia.api.pub.CustomCodeAction;
import com.zerog.ia.api.pub.InstallerProxy;
import com.zerog.ia.api.pub.UninstallerProxy;

import java.io.File;
import java.io.FileFilter;

public class ListWarApplications extends CustomCodeAction {

  public void install(InstallerProxy ip) {
    String warDirPath = (String) ip.getVariable("WAR_FOLDER");
    FileFilter filter = new WarFileFilter();
    File[] warList = new File(warDirPath).listFiles(filter);
    for (int i = 0; i < warList.length; i++) {
      ip.setVariable("USR_WAR_" + i, ip.substitute(warList[i].getName()));
    }
  }

  public void uninstall(UninstallerProxy up) {
    // not implemented
  }

  public String getInstallStatusMessage() {
    return "Validating WAR Files";
  }

  /**
   * This method will be called to display a status message during the uninstall.
   */
  public String getUninstallStatusMessage() {
    return "";
  }
}
