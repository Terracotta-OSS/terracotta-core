/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.gb;

import com.tc.exception.TCRuntimeException;
import com.tc.objectserver.api.ClassPersistor;
import java.nio.ByteBuffer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;
import org.terracotta.corestorage.heap.KeyValueStorageConfigImpl;

public class GBClassPersistorImpl implements ClassPersistor {

  private final StorageManager mgr;
  private KeyValueStorage<Integer,ByteBuffer> clazzes;
  private static final String mapName = "GB_CLASS_STORAGE";

  public GBClassPersistorImpl(StorageManager mgr) {
    super();
    this.mgr = mgr;
    this.clazzes = mgr.getKeyValueStorage(mapName, Integer.class, ByteBuffer.class);
    if ( this.clazzes == null ) {
        this.clazzes = mgr.createKeyValueStorage(mapName, getConfig());
    }
  }
  
  private KeyValueStorageConfig<Integer, ByteBuffer> getConfig() {
      return new KeyValueStorageConfigImpl<Integer, ByteBuffer>(Integer.class, ByteBuffer.class);
      
  }

  public void storeClass(int clazzId, byte[] clazzBytes) {
    clazzes.put(Integer.valueOf(clazzId), ByteBuffer.wrap(clazzBytes));
  }

  public byte[] retrieveClass(int clazzId) {
    ByteBuffer clazzbytes = clazzes.get(clazzId);
    if (clazzbytes == null) { throw new TCRuntimeException("Class bytes not found : " + clazzId); }
    if ( clazzbytes.hasArray() ) {
        return clazzbytes.array();
    } else {
        byte[] cb = new byte[clazzbytes.remaining()];
        clazzbytes.get(cb);
        return cb;
    }
  }

  public Map retrieveAllClasses() {
    Set<Integer> keys = clazzes.keySet();
    HashMap map = new HashMap<Integer,byte[]>();
    for (Integer i : keys ) {
        map.put(i, retrieveClass(i));
    }
    return map;
  }

}