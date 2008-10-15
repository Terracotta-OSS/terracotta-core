/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

public interface InstallListener {

  public enum InstallNotification {
    STARTING, ABORTED, SKIPPED, DOWNLOAD_FAILED, INSTALL_FAILED, INSTALLED;
  }

  void notify(Object source, InstallNotification type, String message);

}
