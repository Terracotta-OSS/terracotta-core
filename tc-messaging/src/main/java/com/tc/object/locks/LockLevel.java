/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.object.locks;

public enum LockLevel {
  READ, WRITE,
  SYNCHRONOUS_WRITE,
  CONCURRENT;

  public static final int READ_LEVEL              = 1;
  public static final int WRITE_LEVEL             = 2;
  public static final int SYNCHRONOUS_WRITE_LEVEL = 3;
  public static final int CONCURRENT_LEVEL        = 4;

  public boolean isWrite() {
    switch (this) {
      case WRITE:
      case SYNCHRONOUS_WRITE:
        return true;
      //$CASES-OMITTED$
      default:
        return false;
    }
  }
  
  public boolean isRead() {
    switch (this) {
      case READ:
        return true;
      //$CASES-OMITTED$
      default:
        return false;
    }
  }
  
  public boolean isSyncWrite() {
    return SYNCHRONOUS_WRITE.equals(this);
  }
  
  public int toInt() {
    switch (this) {
      case READ:
        return READ_LEVEL;
      case WRITE:
        return WRITE_LEVEL;
      case SYNCHRONOUS_WRITE:
        return SYNCHRONOUS_WRITE_LEVEL;
      case CONCURRENT:
        return CONCURRENT_LEVEL;
      default:
        throw new AssertionError("Enum semantics broken in LockLevel?");
    }

  }

  public static LockLevel fromInt(int integer) {
    switch (integer) {
      case READ_LEVEL:
        return READ;
      case WRITE_LEVEL:
        return WRITE;
      case SYNCHRONOUS_WRITE_LEVEL:
        return SYNCHRONOUS_WRITE;
      case CONCURRENT_LEVEL:
        return CONCURRENT;
      default:
        throw new IllegalArgumentException("Invalid integer lock level " + integer);
    }
  }  
}
