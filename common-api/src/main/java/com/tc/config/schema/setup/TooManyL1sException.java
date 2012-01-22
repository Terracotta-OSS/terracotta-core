/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup;

/**
 * Created by brownie Date: Dec 22, 2005 Time: 10:30:14 AM
 */
public class TooManyL1sException extends Exception {

  public TooManyL1sException() {
    //
  }

  public TooManyL1sException(String message) {
    super(message);
  }

  public TooManyL1sException(Throwable cause) {
    super(cause);
  }

  public TooManyL1sException(String message, Throwable cause) {
    super(message, cause);
  }
}
