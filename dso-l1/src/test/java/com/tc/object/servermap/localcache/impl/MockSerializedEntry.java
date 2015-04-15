/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.object.ObjectID;
import com.tc.object.TCClass;
import com.tc.object.TCObjectSelfImpl;

import java.util.Arrays;

public class MockSerializedEntry extends TCObjectSelfImpl {
  private final byte[] myArray;
  private long         lastAccessed;

  public MockSerializedEntry(ObjectID id, byte[] array, TCClass tcClazz) {
    this(id, array, tcClazz, System.currentTimeMillis());
  }

  public MockSerializedEntry(ObjectID id, byte[] array, TCClass tcClazz, long lastAccessedTime) {
    this.initializeTCObject(id, tcClazz, false);
    this.myArray = array;
    this.lastAccessed = lastAccessedTime;
  }

  public synchronized void setLastAccessedTime(long time) {
    this.lastAccessed = time;
  }

  public synchronized long getLastAccessedTime() {
    return lastAccessed;
  }

  public synchronized byte[] getSerializedBytes() {
    return this.myArray;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (lastAccessed ^ (lastAccessed >>> 32));
    result = prime * result + Arrays.hashCode(myArray);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    MockSerializedEntry other = (MockSerializedEntry) obj;
    if (!Arrays.equals(myArray, other.myArray)) return false;
    return true;
  }
}