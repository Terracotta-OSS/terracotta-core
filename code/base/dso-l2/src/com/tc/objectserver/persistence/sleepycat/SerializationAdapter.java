/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.DatabaseEntry;
import com.tc.objectserver.core.api.ManagedObject;

import java.io.IOException;

public interface SerializationAdapter {
  public void serializeManagedObject(DatabaseEntry entry, ManagedObject managedObject) throws IOException;
  public void serializeString(DatabaseEntry entry, String string) throws IOException;
  
  public ManagedObject deserializeManagedObject(DatabaseEntry data) throws IOException, ClassNotFoundException;
  public String deserializeString(DatabaseEntry data) throws IOException, ClassNotFoundException;
}
