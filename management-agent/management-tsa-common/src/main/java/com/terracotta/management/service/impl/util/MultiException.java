/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Ludovic Orban
 */
public class MultiException extends Exception {

  private final List<Throwable> throwables;

  public MultiException(String message, List<Throwable> throwables) {
    super(message);
    this.throwables = Collections.unmodifiableList(new ArrayList<Throwable>(throwables));
  }

  public List<Throwable> getThrowables() {
    return throwables;
  }

  public String getMessage() {
    StringBuilder errorMessage = new StringBuilder();
    errorMessage.append(super.getMessage());
    errorMessage.append("; collected ");
    errorMessage.append(throwables.size());
    errorMessage.append(" exception(s):");

    for (Throwable throwable : throwables) {
      errorMessage.append(System.getProperty("line.separator"));
      errorMessage.append(" [");
      errorMessage.append(throwable.getClass().getName());
      errorMessage.append(" - ");
      errorMessage.append(throwable.getMessage());
      errorMessage.append("]");
    }
    return errorMessage.toString();
  }

}
