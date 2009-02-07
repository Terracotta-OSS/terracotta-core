package com.tc.license.util;

public class LicenseException extends Exception {
  private static final long serialVersionUID = -8934609107742275275L;

  public LicenseException(String message) {
    super(message);
  }

  public LicenseException(Throwable cause) {
    super(cause);
  }

  public LicenseException(String message, Throwable cause) {
    super(message, cause);
  }
}
