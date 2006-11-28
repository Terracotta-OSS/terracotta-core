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