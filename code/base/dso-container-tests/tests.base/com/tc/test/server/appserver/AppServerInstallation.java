/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver;

import java.io.File;

/**
 * Represents an application server installation. Instantiated implementations should be shared across multiple
 * appservers.
 */
public interface AppServerInstallation {

  void uninstall() throws Exception;

  File getDataDirectory();

  File getSandboxDirectory();
}