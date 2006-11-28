/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.integrationtests.framework;

public class WARDeployment implements Deployment {

  private final FileSystemPath warFile;

  public WARDeployment(FileSystemPath warFile) {
    this.warFile = warFile;
  }

  public FileSystemPath getFileSystemPath() {
    return warFile;
  }

}
