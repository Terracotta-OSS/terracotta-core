/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.glassfishv1;

import com.tc.test.server.appserver.StandardAppServerParameters;

import java.util.Properties;

/**
 * DSO specific arguments for GlassfishV1
 */
public final class GlassfishV1AppServerParameters extends StandardAppServerParameters {

  public GlassfishV1AppServerParameters(String instanceName, Properties props, String tcSessionClasspath) {
    super(instanceName, props, tcSessionClasspath);
  }

}
