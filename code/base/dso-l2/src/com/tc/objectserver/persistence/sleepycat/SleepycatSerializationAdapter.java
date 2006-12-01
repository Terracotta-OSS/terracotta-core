/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.je.DatabaseEntry;
import com.tc.objectserver.core.api.ManagedObject;

import java.util.HashMap;
import java.util.Map;

public class SleepycatSerializationAdapter implements SerializationAdapter {

  private final ClassCatalog classCatalog;
  private final Map entryBindings;

  public SleepycatSerializationAdapter(ClassCatalog classCatalog) {
    this.classCatalog = classCatalog;
    this.entryBindings = new HashMap();
  }
  
  public void serializeManagedObject(DatabaseEntry entry, ManagedObject mo) {
    serialize(entry, mo, ManagedObject.class);
  }
  
  public void serializeString(DatabaseEntry entry, String string) {
    serialize(entry, string, String.class);
  }
  
  public ManagedObject deserializeManagedObject(DatabaseEntry entry) {
    return (ManagedObject) deserialize(entry, ManagedObject.class);
  }
  
  public String deserializeString(DatabaseEntry entry) {
    return (String)deserialize(entry, String.class);
  }
  
  public void reset() {
    return;
  }
  
  private void serialize(DatabaseEntry entry, Object o, Class clazz) {
    getEntryBindingFor(clazz).objectToEntry(o, entry);
  }

  private Object deserialize(DatabaseEntry data, Class clazz) {
    return getEntryBindingFor(clazz).entryToObject(data);
  }

  private EntryBinding getEntryBindingFor(Class c) {
    EntryBinding rv = (EntryBinding) this.entryBindings.get(c);
    if (rv == null) {
      rv = new SerialBinding(this.classCatalog, c);
      this.entryBindings.put(c, rv);
    }
    return rv;
  }

}
