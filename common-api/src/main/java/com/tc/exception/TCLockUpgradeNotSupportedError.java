/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.exception;

/**
 * Thrown when someone tries to call an unimplemented feature.
 */
public class TCLockUpgradeNotSupportedError extends TCError {
  public final static String  CLASS_SLASH = "com/tc/exception/TCLockUpgradeNotSupportedError";
  
  private static final ExceptionWrapper wrapper = new ExceptionWrapperImpl();

  private static final String PRETTY_TEXT = "Lock upgrade is not supported. The READ lock needs to be unlocked before a WRITE lock can be requested. \n";

  public TCLockUpgradeNotSupportedError() {
    this(PRETTY_TEXT);
  }

  public TCLockUpgradeNotSupportedError(String message) {
    super(wrapper.wrap(message));
  }

  public TCLockUpgradeNotSupportedError(Throwable cause) {
    this(PRETTY_TEXT, cause);
  }

  public TCLockUpgradeNotSupportedError(String message, Throwable cause) {
    super(wrapper.wrap(message), cause);
  }

}
