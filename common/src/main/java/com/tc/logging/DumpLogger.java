package com.tc.logging;

import org.slf4j.Logger;

public class DumpLogger extends BaseMessageDecoratorTCLogger {

  public DumpLogger(Logger logger) {
    super(logger);
  }

  @Override
  protected String decorate(Object message) {
    return "[dump] " + message;
  }
}
