/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.installer.action;

import com.zerog.ia.api.pub.CustomCodeAction;
import com.zerog.ia.api.pub.InstallerProxy;
import com.zerog.ia.api.pub.UninstallerProxy;

public class SetHomeDir extends CustomCodeAction {

  public void install(InstallerProxy ip) {
    String homeDir = ip.getVariable("USER_INSTALL_DIR").toString();
    if (homeDir.endsWith("\\")) homeDir = homeDir.substring(0, homeDir.length() -1);
    else if (homeDir.endsWith("/")) homeDir = homeDir.substring(0, homeDir.length() -1);
    ip.setVariable("USR_HOME", ip.substitute(homeDir));
  }

  public void uninstall(UninstallerProxy up) {
    // not implemented
  }

  public String getInstallStatusMessage() {
    return "";
  }

  public String getUninstallStatusMessage() {
    return "";
  }
}
