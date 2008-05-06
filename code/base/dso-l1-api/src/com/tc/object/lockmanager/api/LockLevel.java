/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.api;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Lock level constants
 */
public class LockLevel {
  /** NOTE: The NIL level isn't a valid lock level. It used to indicate the absence of a defined/valid lock level */
  public final static int  NIL_LOCK_LEVEL    = 0;

  /** READ bit */
  public final static int  READ              = 1;
  /** WRITE bit */
  public final static int  WRITE             = 2;
  /** CONCURRENT bit */
  public final static int  CONCURRENT        = 4;

  /** GREEDY bit */
  private final static int GREEDY            = 0x80;
  /** SYNCHRONOUS bit, used with WRITE as SYNCHRONOUS_WRITE */
  private final static int SYNCHRONOUS       = 0X40;

  /** Combination of WRITE with SYNCHRONOUS flag */
  public final static int  SYNCHRONOUS_WRITE = WRITE | SYNCHRONOUS;

  private LockLevel() {
    // not to be instantiated
  }

  /**
   * @param level Level
   * @return Is lock level READ?
   */
  public static boolean isRead(int level) {
    if (level <= 0) return false;
    return (level & READ) == READ;
  }

  /**
   * @param level Level
   * @return Is lock level WRITE?
   */
  public static boolean isWrite(int level) {
    if (level <= 0) return false;
    return (level & WRITE) == WRITE;
  }

  /**
   * @param level Level
   * @return Is lock level CONCURRENT?
   */
  public static boolean isConcurrent(int level) {
    if (level <= 0) return false;
    return (level & CONCURRENT) == CONCURRENT;
  }

  /**
   * @param level Level
   * @return Is lock level SYNCHRONOUS_WRITE?
   */
  public static boolean isSynchronousWrite(int level) {
    if (level <= 0) return false;
    return (level & SYNCHRONOUS_WRITE) == SYNCHRONOUS_WRITE;
  }

  /**
   * @param level Level
   * @return Is lock level GREEDY?
   */
  public static boolean isGreedy(int level) {
    if (level <= 0) return false;
    return (level & GREEDY) == GREEDY;
  }

  /**
   * @param level Level
   * @return Is SYNCHRONOUS?
   */
  public static boolean isSynchronous(int level) {
    if (level <= 0) return false;
    return (level & SYNCHRONOUS) == SYNCHRONOUS;
  }

  /**
   * @param level Lock level
   * @return String representation
   */
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

    if (isSynchronousWrite(level)) {
      levels.add("SYNCHRONOUS_WRITE");
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
   * @return True if discrete
   */
  public static boolean isDiscrete(int lockLevel) {
    switch (lockLevel) {
      case READ:
      case WRITE:
      case CONCURRENT:
        return true;
      default:
        return false;
    }
  }

  /** 
   * Make lock greedy
   * @param level Start
   * @return Greedy version of level
   */
  public static int makeGreedy(int level) {
    return level | GREEDY;
  }

  /** 
   * Make lock not greedy
   * @param level Start
   * @return Not greedy version of level
   */
  public static int makeNotGreedy(int level) {
    return level & (~GREEDY);
  }

  /** 
   * Make lock synchronous
   * @param level Start
   * @return Synchronous version of level
   */
  public static int makeSynchronous(int level) {
    return level | SYNCHRONOUS;
  }
  
  /** 
   * Make not synchronous
   * @param level Start
   * @return Not synchronous version of level
   */
  public static int makeNotSynchronous(int level) {
    return level & (~SYNCHRONOUS);
  }
}
