/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

public class CommandException extends Exception {

  private Throwable ex;

  public CommandException() {
    super((Throwable) null); // Disallow initCause
  }

  public CommandException(String s) {
    super(s, null); // Disallow initCause
  }

  public CommandException(String s, Throwable ex) {
    super(s, null); // Disallow initCause
    this.ex = ex;
  }

  public Throwable getException() {
    return ex;
  }

  public Throwable getCause() {
    return ex;
  }
}
