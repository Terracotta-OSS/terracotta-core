/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import com.tc.object.locks.LockLevel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Describe a lock level from a set of enumerated values.  Use the static constants or
 * the static factory method {@link #lockLevelByName(String)} to get an instance.  There are 
 * 8 types of locks defined in the config - these can all be applied to auto locks, but 
 * only the ones without AUTO_SYNCHRONIZED prefix can be used with named locks.  
 * 
 * From a concurrency perspective, there are four levels of locking that allow different
 * amounts of access to a section:
 * <ul>
 * <li>WRITE - like Java synchronized sections - only one thread in the cluster may enter</li>
 * <li>READ - allow either one writer or multiple readers at any given time</li>
 * <li>CONCURRENT - perform no locking, but serve to mark memory transaction boundaries</li>
 * <li>SYNCHRONOUS_WRITE - like WRITE but do not commit until the data has been saved to disk</li>
 * </ul>
 * 
 * In addition, for autolocks, there is an extra attribute called "auto-synhcronized" that indicates
 * that the method should be made synchronized before auto-locking.  This is useful in code you don't have
 * control over that needs to be be made synchronized for use in a clustered environment.
 */
public class ConfigLockLevel {
  static final String                 WRITE_NAME                               = "write";
  static final String                 READ_NAME                                = "read";
  static final String                 CONCURRENT_NAME                          = "concurrent";
  static final String                 SYNCHRONOUS_WRITE_NAME                   = "synchronous-write";
  static final String                 AUTO_SYNCHRONIZED_WRITE_NAME             = "auto-synchronized-write";
  static final String                 AUTO_SYNCHRONIZED_READ_NAME              = "auto-synchronized-read";
  static final String                 AUTO_SYNCHRONIZED_CONCURRENT_NAME        = "auto-synchronized-concurrent";
  static final String                 AUTO_SYNCHRONIZED_SYNCHRONOUS_WRITE_NAME = "auto-synchronized-synchronous-write";

  /** WRITE lock, auto-synchronize=false */
  public static final ConfigLockLevel WRITE                                    = new ConfigLockLevel(WRITE_NAME,
                                                                                                     LockLevel.WRITE);
  /** READ lock, auto-synchronize=false */
  public static final ConfigLockLevel READ                                     = new ConfigLockLevel(READ_NAME,
                                                                                                     LockLevel.READ);
  /** CONCURRENT lock, auto-synchronize=false */
  public static final ConfigLockLevel CONCURRENT                               = new ConfigLockLevel(
                                                                                                     CONCURRENT_NAME,
                                                                                                     LockLevel.CONCURRENT);
  /** SYNCHRONOUS_WRITE lock, auto-synchronize=false */
  public static final ConfigLockLevel SYNCHRONOUS_WRITE                        = new ConfigLockLevel(
                                                                                                     SYNCHRONOUS_WRITE_NAME,
                                                                                                     LockLevel.SYNCHRONOUS_WRITE);
  /** WRITE lock, auto-synchronize=true */
  public static final ConfigLockLevel AUTO_SYNCHRONIZED_WRITE                  = new ConfigLockLevel(
                                                                                                     AUTO_SYNCHRONIZED_WRITE_NAME,
                                                                                                     LockLevel.WRITE);
  /** READ lock, auto-synchronize=false */
  public static final ConfigLockLevel AUTO_SYNCHRONIZED_READ                   = new ConfigLockLevel(
                                                                                                     AUTO_SYNCHRONIZED_READ_NAME,
                                                                                                     LockLevel.READ);
  /** CONCURRENT lock, auto-synchronize=false */
  public static final ConfigLockLevel AUTO_SYNCHRONIZED_CONCURRENT             = new ConfigLockLevel(
                                                                                                     AUTO_SYNCHRONIZED_CONCURRENT_NAME,
                                                                                                     LockLevel.CONCURRENT);
  /** SYNCHRONOUS_WRITE lock, auto-synchronize=false */
  public static final ConfigLockLevel AUTO_SYNCHRONIZED_SYNCHRONOUS_WRITE      = new ConfigLockLevel(
                                                                                                     AUTO_SYNCHRONIZED_SYNCHRONOUS_WRITE_NAME,
                                                                                                     LockLevel.SYNCHRONOUS_WRITE);

  private static final Map            locksByLevel;

  static {
    HashMap tmp = new HashMap();

    tmp.put(WRITE_NAME, WRITE);
    tmp.put(READ_NAME, READ);
    tmp.put(CONCURRENT_NAME, CONCURRENT);
    tmp.put(SYNCHRONOUS_WRITE_NAME, SYNCHRONOUS_WRITE);
    tmp.put(AUTO_SYNCHRONIZED_WRITE_NAME, AUTO_SYNCHRONIZED_WRITE);
    tmp.put(AUTO_SYNCHRONIZED_READ_NAME, AUTO_SYNCHRONIZED_READ);
    tmp.put(AUTO_SYNCHRONIZED_CONCURRENT_NAME, AUTO_SYNCHRONIZED_CONCURRENT);
    tmp.put(AUTO_SYNCHRONIZED_SYNCHRONOUS_WRITE_NAME, AUTO_SYNCHRONIZED_SYNCHRONOUS_WRITE);

    locksByLevel = Collections.unmodifiableMap(tmp);
  }

  private final String                lockLevelName;
  private final LockLevel             level;

  private ConfigLockLevel(String lockTypeName, LockLevel type) {
    this.lockLevelName = lockTypeName;
    this.level = type;
  }

  /**
   * @return Ordinal lock level value
   */
  public int getLockLevelAsInt() {
    return level.toInt();
  }

  public String toString() {
    return lockLevelName;
  }

  /**
   * Provide an instance of the constant for the specified name
   * or null if name is invalid
   * @param typeName Lock level name
   * @return Lock level instance
   */
  public static ConfigLockLevel lockLevelByName(String typeName) {
    ConfigLockLevel rv = null;
    if (typeName != null) rv = (ConfigLockLevel) locksByLevel.get(typeName);
    return rv;
  }
}
