package com.tc.lang;

import com.tc.logging.TCLogger;

/**
* @author tim
*/
public class TestThrowableHandler extends ThrowableHandlerImpl {
  private volatile Throwable throwable;

  /**
   * Construct a new ThrowableHandler with a logger
   *
   * @param logger Logger
   */
  public TestThrowableHandler(final TCLogger logger) {
    super(logger);
  }

  @Override
  public void handleThrowable(final Thread thread, final Throwable t) {
    this.throwable = t;
    super.handleThrowable(thread, t);
  }

  public void throwIfNecessary() throws Throwable {
    if (throwable != null) { throw throwable;
    }
  }

  @Override
  protected synchronized void exit(final int status) {
    // don't do a system.exit.
  }
}
