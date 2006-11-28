/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.test.server.util;

import com.tc.util.PortChooser;

public class AppServerUtil {

  private static final PortChooser pc = new PortChooser();

  public static int getPort() throws Exception {
    return pc.chooseRandomPort();
  }
}
