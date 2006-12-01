/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.dsoserver;

import com.tc.test.server.ServerParameters;

import java.io.File;

/**
 * Server parameters specific to DSO. These methods are read by the DsoServer implementation.
 */
public interface DsoServerParameters extends ServerParameters {

  File configFile();

  int dsoPort();

  int jmxPort();

  File workingDir();

  File outputFile();
}
