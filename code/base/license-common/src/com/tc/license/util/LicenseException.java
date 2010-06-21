/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.license.util;

public class LicenseException extends RuntimeException {
  public LicenseException() {
    super();
  }

  public LicenseException(String message) {
    super(message);
  }

  public LicenseException(String message, Throwable cause) {
    super(message, cause);
  }

  public LicenseException(Throwable cause) {
    super(cause);
  }
}
