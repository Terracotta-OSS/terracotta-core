/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.lock;

import java.util.ArrayList;
import java.util.Iterator;

public class LockLevel {
  // NOTE: The NIL level isn't a valid lock level. It used to indicate the absence of a defined/valid lock level
  public final static int  NIL_LOCK_LEVEL = 0;

  public final static int  READ           = 1;
  public final static int  WRITE          = 2;
  public final static int  CONCURRENT     = 4;

  private final static int GREEDY         = 0x80;

  private LockLevel() {
    // not to be instantiated
  }

  public static boolean isWrite(int level) {
    if (level <= 0) return false;
    return (level & WRITE) == WRITE;
  }

  public static boolean isRead(int level) {
    if (level <= 0) return false;
    return (level & READ) == READ;
  }

  public static boolean isConcurrent(int level) {
    if (level <= 0) return false;
    return (level & CONCURRENT) == CONCURRENT;
  }

  public static boolean isGreedy(int level) {
    if (level <= 0) return false;
    return (level & GREEDY) == GREEDY;
  }
  
  public static String toString(int level) {
    ArrayList levels = new ArrayList();
    StringBuffer rv = new StringBuffer();

    if (isRead(level)) {
      levels.add("READ");
    }

    if (isWrite(level)) {
      levels.add("WRITE");
    }

    if (isConcurrent(level)) {
      levels.add("CONCURRENT");
    }

    if (levels.size() == 0) {
      levels.add("UNKNOWN:" + level);
    }

    for (Iterator iter = levels.iterator(); iter.hasNext();) {
      rv.append(iter.next()).append(' ');
    }

    rv.append('(').append(level).append(')');

    return rv.toString();

  }

  /**
   * Is this a discrete lock level? A lock level which is a combination (like READ+WRITE) is non-discreet
   */
  public static boolean isDiscrete(int lockLevel) {
    switch (lockLevel) {
      case WRITE:
      case READ:
      case CONCURRENT:
        return true;
      default:
        return false;
    }
  }

  public static int makeGreedy(int level) {
    return level | GREEDY;
  }

  public static int makeNotGreedy(int level) {
    return level ^ GREEDY ;
  }
}
