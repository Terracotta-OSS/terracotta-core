/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
