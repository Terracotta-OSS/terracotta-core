/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.concurrent;

/**
 * Some shortcut stuff for doing common thread stuff
 * 
 * @author steve
 */
public class ThreadUtil {

  public static void reallySleep(long millis) {
    reallySleep(millis, 0);
  }
  
  public static void reallySleep(long millis, int nanos) {
    boolean interrupted = false;
    try {
      long millisLeft = millis;
      while (millisLeft > 0 || nanos > 0) {
        long start = System.currentTimeMillis();
        try {
          Thread.sleep(millisLeft, nanos);
        } catch (InterruptedException e) {
          interrupted = true;
        }
        millisLeft -= System.currentTimeMillis() - start;
        nanos = 0 ; // Not using System.nanoTime() since it is 1.5 specific
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * @return <code>true</code> if the call to Thread.sleep() was successful, <code>false</code> if the call was
   *         interrupted.
   */
  public static boolean tryToSleep(long millis) {
    boolean slept = false;
    try {
      Thread.sleep(millis);
      slept = true;
    } catch (InterruptedException ie) {
      slept = false;
    }
    return slept;
  }

  public static void printStackTrace(StackTraceElement ste[]) {
    for (int i = 0; i < ste.length; i++) {
      System.err.println("\tat " + ste[i]);
    }
  }
}