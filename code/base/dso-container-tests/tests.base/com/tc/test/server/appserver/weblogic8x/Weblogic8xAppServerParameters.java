/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.test.server.appserver.weblogic8x;

import com.tc.test.server.appserver.StandardAppServerParameters;

import java.util.Properties;

/**
 * DSO specific arguments for weblogic8x appservers.
 */
public final class Weblogic8xAppServerParameters extends StandardAppServerParameters {

  public Weblogic8xAppServerParameters(String instanceName, Properties props, String tcSessionClasspath) {
    super(instanceName, props, tcSessionClasspath);
  }

}
