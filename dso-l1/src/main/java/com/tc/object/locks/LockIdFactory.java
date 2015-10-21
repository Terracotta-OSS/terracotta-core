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

public class LockIdFactory {

  public LockIdFactory() {
    //
  }

  public LockID generateLockIdentifier(Object obj) {
    if (obj instanceof LockID) {
      return (LockID) obj;
    }
    
    if (obj instanceof Long) {
      return generateLockIdentifier(((Long) obj).longValue());
    } else if (obj instanceof String) {
      return generateLockIdentifier((String) obj);
    } else {
      throw new AssertionError("unsupported type: " + obj.getClass());
    }
  }
  
  private LockID generateLockIdentifier(long l) {
    return new LongLockID(l);
  }

  private LockID generateLockIdentifier(String str) {
    return new StringLockID(str);
  }
}
