/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.license;

/**
 * Thrown when a license is invalid.
 */
public class InvalidLicenseException extends Exception {

  public InvalidLicenseException() {
    super();
  }

  public InvalidLicenseException(String message) {
    super(message);
  }

  public InvalidLicenseException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidLicenseException(Throwable cause) {
    super(cause);
  }

}
