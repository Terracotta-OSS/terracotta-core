/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.objectserver.core.api.ManagedObject;

import java.io.IOException;

public interface SerializationAdapter {
  public byte[] serializeManagedObject(ManagedObject managedObject) throws IOException;
  public byte[] serializeString(String string) throws IOException;
  
  public ManagedObject deserializeManagedObject(byte[] data) throws IOException, ClassNotFoundException;
  public String deserializeString(byte[] data) throws IOException, ClassNotFoundException;
}
