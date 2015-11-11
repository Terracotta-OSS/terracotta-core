package org.terracotta.passthrough;

import java.lang.Thread.UncaughtExceptionHandler;


/**
 * The uncaught exception handler installed for all the threads in the passthrough testing system.  All it does is log the
 * error and terminate the VM.
 */
public class PassthroughUncaughtExceptionHandler implements UncaughtExceptionHandler {
  public static final PassthroughUncaughtExceptionHandler sharedInstance = new PassthroughUncaughtExceptionHandler();

  @Override
  public void uncaughtException(Thread arg0, Throwable arg1) {
    System.err.println("FATAL EXCEPTION IN PASSTHROUGH THREAD:");
    arg1.printStackTrace();
    System.exit(1);
  }
}
