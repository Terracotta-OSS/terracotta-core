/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.deployment;


public class WARDeployment implements Deployment {

  private final FileSystemPath warFile;

  public WARDeployment(FileSystemPath warFile) {
    this.warFile = warFile;
  }

  public FileSystemPath getFileSystemPath() {
    return warFile;
  }

}
