/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver;

import java.io.File;

/**
 * This interface is to be called by implementations of {@link AbstractAppServer} only. Do not typecast concrete
 * installations to this type! Do not call these methods under any circumstances.
 */
interface AppServerStartupEnvironment extends AppServerInstallation {

  File serverBaseDir();

  File workingDirectory();

  File serverInstallDirectory();

  String serverType();

  String majorVersion();

  String minorVersion();

  boolean isRepoInstall();
}
