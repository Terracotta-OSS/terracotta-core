/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
