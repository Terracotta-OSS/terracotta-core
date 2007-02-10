/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.migrate;

public class ConfigUpdateException extends RuntimeException {

  public ConfigUpdateException() {
    super();
  }
  
  public ConfigUpdateException(String msg) {
    super(msg);
  }
  
  public ConfigUpdateException(String msg, Throwable t) {
    super(msg, t);
  }
}
