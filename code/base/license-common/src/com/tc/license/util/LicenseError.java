/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.license.util;

public class LicenseError extends RuntimeException {
  public LicenseError() {
    super();
  }

  public LicenseError(String message) {
    super(message);
  }

  public LicenseError(String message, Throwable cause) {
    super(message, cause);
  }

  public LicenseError(Throwable cause) {
    super(cause);
  }
}
