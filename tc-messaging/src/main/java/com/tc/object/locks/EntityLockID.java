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

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.EntityID;

import java.io.IOException;

/**
 * @author twu
 */
public class EntityLockID implements LockID {
  private final EntityID entityID;

  EntityLockID() {
    this("UNKNOWN", "UNKNOWN");
  }

  public EntityLockID(String className, String entityName) {
    this(new EntityID(className, entityName));
  }

  public EntityLockID(EntityID entityID) {
    this.entityID = entityID;
  }

  @Override
  public LockIDType getLockType() {
    return LockIDType.ENTITY;
  }

  @Override
  public void serializeTo(TCByteBufferOutput serialOutput) {
    entityID.serializeTo(serialOutput);
  }

  @Override
  public EntityLockID deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    return new EntityLockID(EntityID.NULL_ID.deserializeFrom(serialInput));
  }

  @Override
  public int compareTo(LockID o) {
    if (o instanceof EntityLockID) {
      // TODO: this doesn't quite look right...
      return entityID.getEntityName().compareTo(((EntityLockID) o).entityID.getEntityName());
    }
    throw new IllegalArgumentException("Not an EntityLockID.");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final EntityLockID that = (EntityLockID) o;

    if (!entityID.equals(that.entityID)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return entityID.hashCode();
  }

  @Override
  public String toString() {
    return "EntityLockID{" +
           "entityID=" + entityID +
           '}';
  }
}
