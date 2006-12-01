/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.installer.action;

import org.apache.commons.io.FileUtils;

import com.zerog.ia.api.pub.CustomCodeAction;
import com.zerog.ia.api.pub.InstallerProxy;
import com.zerog.ia.api.pub.UninstallerProxy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class CopyWarApplications extends CustomCodeAction {

  public void install(InstallerProxy ip) {
    ArrayList wars = new ArrayList();
    String warName = null;
    for (int i = 0;; i++) {
      warName = (String) ip.getVariable("USR_CP_WAR_" + i);
      if (warName == null) break;
      System.out.println(warName);
      wars.add(warName);
    }

    Object[] warList = wars.toArray();
    String warDirPath = (String) ip.getVariable("WAR_FOLDER");
    try {
      for (int i = 0; i < warList.length; i++) {
        System.out.println("copy_" + i);
        FileUtils.copyFileToDirectory(new File(warDirPath + File.separator + warList[i].toString()), new File("C:\\Documents and Settings\\installer\\Desktop"));
      }
    } catch (IOException e) {
      // set error variable
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