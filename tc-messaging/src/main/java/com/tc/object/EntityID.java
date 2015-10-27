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

package com.tc.object;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.IOException;

/**
 * @author twu
 */
public class EntityID implements TCSerializable<EntityID> {
  public static final EntityID NULL_ID = new EntityID("UKNONWN", "UNKNOWN");
  
  private final String className;
  private final String entityName;

  public EntityID(String className, String entityName) {
    this.entityName = entityName;
    this.className = className;
  }

  public String getClassName() {
    return className;
  }

  public String getEntityName() {
    return entityName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final EntityID that = (EntityID) o;

    if (!className.equals(that.className)) return false;
    if (!entityName.equals(that.entityName)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = className.hashCode();
    result = 31 * result + entityName.hashCode();
    return result;
  }

  @Override
  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeString(className);
    serialOutput.writeString(entityName);
  }

  @Override
  public EntityID deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    return readFrom(serialInput);
  }

  public static EntityID readFrom(TCByteBufferInput serialInput) throws IOException {
    return new EntityID(serialInput.readString(), serialInput.readString());
  }

  @Override
  public String toString() {
    return "EntityID{" +
           "className='" + className + '\'' +
           ", entityName='" + entityName + '\'' +
           '}';
  }
}
