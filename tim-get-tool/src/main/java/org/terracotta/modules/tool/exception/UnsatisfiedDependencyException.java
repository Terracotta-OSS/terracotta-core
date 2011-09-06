/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.tool.exception;

import org.terracotta.modules.tool.Reference;

public class UnsatisfiedDependencyException extends Exception {

  private final Reference dependency;

  public UnsatisfiedDependencyException(String message, Reference dependency) {
    super(message);
    this.dependency = dependency;
  }

  @Override
  public String toString() {
    return dependency + " : " + getMessage();
  }
}
