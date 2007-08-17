/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.io.serializer.DSOSerializerPolicy;
import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.io.serializer.api.BasicSerializer;
import com.tc.logging.TCLogger;
import com.tc.memorydatastore.client.MemoryDataStoreClient;
import com.tc.object.ObjectID;
import com.tc.util.Conversion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;

public class MemoryStoreCollectionsPersistor {

  private final MemoryDataStoreClient     database;
  private final BasicSerializer           serializer;
  private final ByteArrayOutputStream     bao;
  private final MemoryStoreCollectionFactory collectionFactory;
  private final TCObjectOutputStream      oo;

  public MemoryStoreCollectionsPersistor(TCLogger logger, MemoryDataStoreClient mapsDatabase,
                                      MemoryStoreCollectionFactory memoryStoreCollectionFactory) {
    this.database = mapsDatabase;
    this.collectionFactory = memoryStoreCollectionFactory;
    DSOSerializerPolicy policy = new DSOSerializerPolicy();
    this.serializer = new BasicSerializer(policy);
    this.bao = new ByteArrayOutputStream(1024);
    this.oo = new TCObjectOutputStream(bao);
  }

  public void saveMap(MemoryStorePersistableMap map) throws IOException {
    map.commit(this, database);
  }

  public synchronized byte[] serialize(long id, Object o) throws IOException {
    oo.writeLong(id);
    serializer.serializeTo(o, oo);
    oo.flush();
    byte b[] = bao.toByteArray();
    bao.reset();
    return b;
  }
  
  public synchronized byte[] serialize(long id) throws IOException {
    oo.writeLong(id);
    oo.flush();
    byte b[] = bao.toByteArray();
    bao.reset();
    return b;
  }

  public synchronized byte[] serialize(Object o) throws IOException {
    serializer.serializeTo(o, oo);
    oo.flush();
    byte b[] = bao.toByteArray();
    bao.reset();
    return b;
  }

  public MemoryStorePersistableMap loadMap(ObjectID id) throws IOException,
      ClassNotFoundException {
    MemoryStorePersistableMap map = (MemoryStorePersistableMap) collectionFactory.createPersistentMap(id);
    map.load();
    return map;
  }

  public Object deserialize(int start, byte[] data) throws IOException, ClassNotFoundException {
    if (start >= data.length) return null;
    ByteArrayInputStream bai = new ByteArrayInputStream(data, start, data.length - start);
    ObjectInput ois = new TCObjectInputStream(bai);
    return serializer.deserializeFrom(ois);
  }

  public Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
    return deserialize(0, data);
  }

   public boolean deleteCollection(ObjectID id) {
    byte idb[] = Conversion.long2Bytes(id.toLong());
    database.removeAll(idb);
    return true;
  }

}
