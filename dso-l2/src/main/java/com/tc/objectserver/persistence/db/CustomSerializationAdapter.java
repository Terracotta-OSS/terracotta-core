/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.io.serializer.api.Serializer;
import com.tc.io.serializer.impl.StringUTFSerializer;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.managedobject.ManagedObjectSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CustomSerializationAdapter implements SerializationAdapter {

  private final ByteArrayOutputStream   baout;
  private final TCObjectOutputStream    out;
  private final ManagedObjectSerializer moSerializer;
  private final StringUTFSerializer     stringSerializer;

  public CustomSerializationAdapter(ManagedObjectSerializer moSerializer, StringUTFSerializer stringSerializer) {
    this.moSerializer = moSerializer;
    this.stringSerializer = stringSerializer;
    baout = new ByteArrayOutputStream(4096);
    out = new TCObjectOutputStream(baout);
  }

  public byte[] serializeManagedObject(ManagedObject managedObject) throws IOException {
      return serialize(managedObject, moSerializer);
  }

  public byte[] serializeString(String string) throws IOException {
      return serialize(string, stringSerializer);
  }

  private byte[] serialize(Object o, Serializer serializer) throws IOException {
    serializer.serializeTo(o, out);
    out.flush();
    byte[] temp = baout.toByteArray();
    baout.reset();
    return temp;
  }

  public ManagedObject deserializeManagedObject(byte[] data) throws IOException, ClassNotFoundException {
    return (ManagedObject) deserialize(data, moSerializer);
  }

  public String deserializeString(byte[] data) throws IOException, ClassNotFoundException {
    return (String) deserialize(data, stringSerializer);
  }

  private Object deserialize(byte[] entry, Serializer serializer) throws IOException, ClassNotFoundException {
    ByteArrayInputStream bain = new ByteArrayInputStream(entry);
    TCObjectInputStream in = new TCObjectInputStream(bain);
    return serializer.deserializeFrom(in);
  }
}
