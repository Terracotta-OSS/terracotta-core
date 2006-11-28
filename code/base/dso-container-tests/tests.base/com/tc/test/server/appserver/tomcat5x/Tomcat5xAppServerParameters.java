/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.test.server.appserver.tomcat5x;

import com.tc.test.server.appserver.StandardAppServerParameters;

import java.util.Properties;

/**
 * DSO specific arguments for Tomcat5x appservers.
 */
public final class Tomcat5xAppServerParameters extends StandardAppServerParameters {

  public Tomcat5xAppServerParameters(String instanceName, Properties props, String tcSessionClasspath) {
    super(instanceName, props, tcSessionClasspath);
  }

}
