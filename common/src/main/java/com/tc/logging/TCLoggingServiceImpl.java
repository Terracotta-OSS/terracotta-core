/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

public class TCLoggingServiceImpl implements TCLoggingService {

  @Override
  public TCLogger getLogger(String name) {
    return TCLogging.getLogger(name);
  }

  @Override
  public TCLogger getLogger(Class c) {
    return TCLogging.getLogger(c);
  }

}
