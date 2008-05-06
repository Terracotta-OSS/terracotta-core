/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.exception.ExceptionWrapper;
import com.tc.exception.ExceptionWrapperImpl;
import com.tc.logging.TCLogger;
import java.lang.reflect.Array;

/**
 * Generic utility methods.
 */
public class Util {
  private static final ExceptionWrapper wrapper = new ExceptionWrapperImpl();
  
  private static final Error FATAL_ERROR = new Error(
  "Fatal error -- Please refer to console output and Terracotta log files for more information");

  /**
   * Enumerates the argument, provided that it is an array, in a nice, human-readable format.
   * 
   * @param array
   * @return
   */
  public static String enumerateArray(Object array) {
    StringBuffer buf = new StringBuffer();
    if (array != null) {
      if (array.getClass().isArray()) {
        for (int i = 0, n = Array.getLength(array); i < n; i++) {
          if (i > 0) {
            buf.append(", ");
          }
          buf.append("<<" + Array.get(array, i) + ">>");
        }
      } else {
        buf.append("<").append(array.getClass()).append(" is not an array>");
      }
    } else {
      buf.append("null");
    }
    return buf.toString();
  }

  public static void printLogAndRethrowError(Throwable t, TCLogger logger) {
    printLogAndMaybeRethrowError(t, true, logger);
  }

  public static void printLogAndMaybeRethrowError(final Throwable t, final boolean rethrow, final TCLogger logger) {
    // if (t instanceof ReadOnlyException) { throw (ReadOnlyException) t; }
    // if (t instanceof UnlockedSharedObjectException) { throw (UnlockedSharedObjectException) t; }
    // if (t instanceof TCNonPortableObjectError) { throw (TCNonPortableObjectError) t; }

    try {
      if (t != null) t.printStackTrace();
      logger.error(t);
    } catch (Throwable err) {
      try {
        err.printStackTrace();
      } catch (Throwable err2) {
        // sorry, game over, stop trying
      }
    } finally {
      if (rethrow) {
        // don't wrap existing Runtime and Error
        if (t instanceof RuntimeException) { throw (RuntimeException) t; }
        if (t instanceof Error) { throw (Error) t; }

        // Try to new up a RuntimeException to throw
        final RuntimeException re;
        try {
          re = new RuntimeException("Unexpected Error " + t.getMessage(), t);
        } catch (Throwable err3) {
          try {
            err3.printStackTrace();
          } catch (Throwable err4) {
            // sorry, game over, stop trying
          }
          throw FATAL_ERROR;
        }

        throw re;
      }
    }
  }
  
  public static void selfInterruptIfNeeded(boolean isInterrupted) {
    if (isInterrupted) {
      Thread.currentThread().interrupt();
    }
  }
  
  public static long getMillis(long timeInNanos) {
    return timeInNanos/1000000;
  }
  
  public static int getNanos(long timeInNanos, long mills) {
    return (int)(timeInNanos - mills*1000000);
  }
  
  public static long getTimeInNanos(long mills, int nanos) {
    return mills*1000000+nanos;
  }
  
  public static int hash(Object key, int limit) {
    if (limit == 1) { return 0; }
    int hashValue = hash(key.hashCode());
    if (hashValue == Integer.MIN_VALUE) { hashValue -= 1; }
    hashValue = Math.abs(hashValue);
    return hashValue % limit;
  }

  private static int hash(int h) {
    h += ~(h << 9);
    h ^= (h >>> 14);
    h += (h << 4);
    h ^= (h >>> 10);
    return h;
  }
  
  public static String getFormattedMessage(String msg) {
    return wrapper.wrap(msg);
  }
}
