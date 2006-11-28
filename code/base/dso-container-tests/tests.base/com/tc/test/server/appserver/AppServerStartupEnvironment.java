/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.test.server.appserver;

import java.io.File;

/**
 * This interface is to be called by implementations of {@link AbstractAppServer} only. Do not typecast concrete
 * installations to this type! Do not call these methods under any circumstances.
 */
interface AppServerStartupEnvironment extends AppServerInstallation {

  File workingDirectory();

  File serverInstallDirectory();

  String serverType();

  String majorVersion();

  String minorVersion();
  
  boolean isRepoInstall();
}
