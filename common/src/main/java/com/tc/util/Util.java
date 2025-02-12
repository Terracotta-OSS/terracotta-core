/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.util;

import org.slf4j.Logger;

import com.tc.exception.ExceptionWrapper;
import com.tc.exception.ExceptionWrapperImpl;

import java.lang.reflect.Array;

/**
 * Generic utility methods.
 */
public class Util {
  private static final ExceptionWrapper wrapper = new ExceptionWrapperImpl();

  private static final Error            FATAL_ERROR = new Error(
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

  @SuppressWarnings("finally")
  public static void printLogAndRethrowError(Throwable t, Logger logger) {
    if (t == null) {
      throw null;
    }
    
    try {
      try {
        t.printStackTrace();
      } finally {
        logger.error("Exception: ", t);
      }
    } catch (Throwable err) {
      try {
        err.printStackTrace();
      } catch (Throwable err2) {
        // sorry, game over, stop trying
      }
    } finally {
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
  
  public static void selfInterruptIfNeeded(boolean isInterrupted) {
    if (isInterrupted) {
      Thread.currentThread().interrupt();
    }
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
