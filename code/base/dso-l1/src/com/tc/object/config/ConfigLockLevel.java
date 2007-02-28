/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import com.tc.object.lockmanager.api.LockLevel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConfigLockLevel {
  static final String                 WRITE_NAME      = "write";
  static final String                 READ_NAME       = "read";
  static final String                 CONCURRENT_NAME = "concurrent";
  static final String                 SYNCHRONOUS_WRITE_NAME = "synchronous-write";

  public static final ConfigLockLevel WRITE           = new ConfigLockLevel(WRITE_NAME, LockLevel.WRITE);
  public static final ConfigLockLevel READ            = new ConfigLockLevel(READ_NAME, LockLevel.READ);
  public static final ConfigLockLevel CONCURRENT      = new ConfigLockLevel(CONCURRENT_NAME, LockLevel.CONCURRENT);
  public static final ConfigLockLevel SYNCHRONOUS_WRITE      = new ConfigLockLevel(SYNCHRONOUS_WRITE_NAME,
                                                                                   LockLevel.SYNCHRONOUS_WRITE);

  private static final Map            locksByLevel;

  static {
    HashMap tmp = new HashMap();

    tmp.put(WRITE_NAME, WRITE);
    tmp.put(READ_NAME, READ);
    tmp.put(CONCURRENT_NAME, CONCURRENT);
    tmp.put(SYNCHRONOUS_WRITE_NAME, SYNCHRONOUS_WRITE);

    locksByLevel = Collections.unmodifiableMap(tmp);
  }

  private final String                lockLevelName;
  private final int                   level;

  private ConfigLockLevel(String lockTypeName, int type) {
    this.lockLevelName = lockTypeName;
    this.level = type;
  }

  public int getLevel() {
    return level;
  }

  public String toString() {
    return lockLevelName;
  }

  public static ConfigLockLevel lockLevelByName(String typeName) {
    ConfigLockLevel rv = null;
    if (typeName != null) rv = (ConfigLockLevel) locksByLevel.get(typeName);
    return rv;
  }
}
