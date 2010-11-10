/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.metadata;

import com.tc.io.TCSerializable;

public interface NVPair extends TCSerializable {

  public abstract String getName();

  public abstract void setName(String aName);

  public abstract String valueAsString();

  public abstract ValueType getType();

  public abstract Object getObjectValue();

}