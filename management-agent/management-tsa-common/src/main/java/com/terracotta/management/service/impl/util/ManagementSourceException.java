/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl.util;

import org.terracotta.management.resource.ErrorEntity;

/**
 * @author Ludovic Orban
 */
public class ManagementSourceException extends RuntimeException {
  private final ErrorEntity errorEntity;

  public ManagementSourceException(Throwable t) {
    super(t);
    errorEntity = null;
  }

  public ManagementSourceException(String msg) {
    super(msg);
    errorEntity = null;
  }

  public ManagementSourceException(String message, ErrorEntity errorEntity) {
    super(message);
    this.errorEntity = errorEntity;
  }

  public ErrorEntity getErrorEntity() {
    return errorEntity;
  }
}
