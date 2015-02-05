package com.tc.object.locks;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;

import java.io.IOException;

/**
 * @author twu
 */
public class EntityLockID implements LockID {
  private final String className;
  private final String entityName;

  EntityLockID() {
    this("UNKNOWN", "UNKNOWN");
  }

  public EntityLockID(String className, String entityName) {
    this.className = className;
    this.entityName = entityName;
  }

  public String getClassName() {
    return className;
  }

  public String getEntityName() {
    return entityName;
  }

  @Override
  public LockIDType getLockType() {
    return LockIDType.ENTITY;
  }

  @Override
  public int compareTo(LockID o) {
    if (o instanceof EntityLockID) {
      return className.compareTo(((EntityLockID) o).getClassName()) + entityName.compareTo(((EntityLockID) o).getEntityName());
    }
    throw new IllegalArgumentException("Not an EntityLockID.");
  }

  @Override
  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeString(className);
    serialOutput.writeString(entityName);
  }

  @Override
  public EntityLockID deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    return new EntityLockID(serialInput.readString(), serialInput.readString());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final EntityLockID that = (EntityLockID) o;

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
}
