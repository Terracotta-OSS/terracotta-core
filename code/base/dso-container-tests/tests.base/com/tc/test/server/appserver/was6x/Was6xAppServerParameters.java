/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.was6x;

import com.tc.test.server.appserver.StandardAppServerParameters;

import java.util.Properties;

/**
 * DSO specific arguments for Websphere6x
 */
public final class Was6xAppServerParameters extends StandardAppServerParameters {

  public Was6xAppServerParameters(String instanceName, Properties props, String tcSessionClasspath) {
    super(instanceName, props, tcSessionClasspath);
  }

}
