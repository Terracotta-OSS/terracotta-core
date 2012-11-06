/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object.serialization;

import org.terracotta.toolkit.ToolkitRuntimeException;

import com.tc.object.TCObject;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.NotClearable;
import com.tc.util.FindbugsSuppressWarnings;

import java.io.IOException;

public class SerializedClusterObjectImpl<T> implements SerializedClusterObject<T>, NotClearable, Manageable {
  /**
   * <pre>
   * ********************************************************************************************
   * IF YOU'RE CHANGING ANYTHING ABOUT THE FIELDS IN THIS CLASS (name, type, add or remove, etc)
   * YOU MUST UPDATE BOTH THE APPLICATOR AND SERVER STATE CLASSES ACCORDINGLY!
   * ********************************************************************************************
   * </pre>
   */
  private volatile byte[]   bytes;
  private T                 value;
  private volatile TCObject tcObject;

  /**
   * Protected constructor - only supposed to be created by the factory
   */
  protected SerializedClusterObjectImpl(final T value, final byte[] bytes) {
    this.value = value;
    this.bytes = bytes;
  }

  @Override
  public synchronized byte[] getBytes() {
    return this.bytes;
  }

  /**
   * TODO: Remember to make byte[] null later
   */
  @Override
  public synchronized T getValue(SerializationStrategy strategy, boolean compression, boolean local) {
    if (value != null) { return value; }
    try {
      value = (T) strategy.deserialize(bytes, compression, local);
      return value;
    } catch (IOException e) {
      throw new ToolkitRuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new ToolkitRuntimeException(e);
    }
  }

  public synchronized void internalSetValue(byte[] bytesParam) {
    this.bytes = bytesParam;
  }

  @Override
  public void __tc_managed(TCObject tco) {
    this.tcObject = tco;
  }

  @Override
  public TCObject __tc_managed() {
    return tcObject;
  }

  @Override
  public boolean __tc_isManaged() {
    return tcObject != null;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SerializedClusterObjectImpl)) { return false; }
    SerializedClusterObjectImpl clusterObjectImpl = (SerializedClusterObjectImpl) obj;

    if (this.tcObject.getObjectID().equals(clusterObjectImpl.tcObject.getObjectID())) { return true; }
    if (this.value != null && value == clusterObjectImpl.value) { return true; }

    return false;
  }

  @Override
  public int hashCode() {
    return this.tcObject.getObjectID().hashCode();
  }

  @Override
  @FindbugsSuppressWarnings("DMI_INVOKING_TOSTRING_ON_ARRAY")
  public String toString() {
    return "SerializedClusterObject [cached=" + value + ", value=" + bytes + "]";
  }
}
