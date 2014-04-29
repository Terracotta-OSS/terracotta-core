/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl.util;

/**
 * @author Ludovic Orban
 */
public class ManagementSourceException extends RuntimeException {
  public ManagementSourceException(Throwable t) {
    super(t);
  }

  public ManagementSourceException(String msg) {
    super(msg);
  }
}
