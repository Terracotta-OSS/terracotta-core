/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;

public class TextDecoratorTCLogger extends BaseMessageDecoratorTCLogger {

  private final String prepend;

  public TextDecoratorTCLogger(TCLogger logger, String prepend) {
    super(logger);
    this.prepend = prepend;
  }

  protected Object decorate(Object message) {
    return prepend + ": " + message;
  }

}
