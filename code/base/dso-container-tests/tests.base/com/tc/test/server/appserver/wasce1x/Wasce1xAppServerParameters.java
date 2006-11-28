/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.test.server.appserver.wasce1x;

import com.tc.test.server.appserver.StandardAppServerParameters;

import java.util.Properties;

/**
 * DSO specific arguments for Wasce1x appservers.
 */
public final class Wasce1xAppServerParameters extends StandardAppServerParameters {

  public Wasce1xAppServerParameters(String instanceName, Properties props, String tcSessionClasspath) {
    super(instanceName, props, tcSessionClasspath);
  }

}
