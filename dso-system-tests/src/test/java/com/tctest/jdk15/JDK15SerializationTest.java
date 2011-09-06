/*
 * Copyright (c) 2003-2008 Terracotta, Inc. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.object.BaseDSOTestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/*
 * TODO:: More tests needed. 1) Populated collections needs to be serialized and deserialized. 2) Serialized
 * instrumented version of collections/objects should be deserializable by uninstrumented versions and vice versa
 */
public class JDK15SerializationTest extends BaseDSOTestCase {
  public void testConcurrentHashMapSerialization() throws Exception {
    ConcurrentHashMap m = new ConcurrentHashMap();

    m.put("key1", "value1");
    m.put("key2", "value2");

    checkSerialization(m);

  }

  public void testLinkedBlockingQueueSerialization() throws Exception {
    LinkedBlockingQueue q = new LinkedBlockingQueue();
    q.put("value1");
    q.put("value2");

    checkSerialization(q);
  }
  
  public void testReentrantLockSerialization() throws Exception {
    ReentrantLock lock = new ReentrantLock();
    
    lock.lock();
    try {
      ReentrantLock deserializedLock = (ReentrantLock)checkSerialization(lock);
      assertFalse(deserializedLock.isLocked());
      assertTrue(lock.isLocked());
    } finally {
      lock.unlock();
    }
  }

  private Object checkSerialization(Object o) throws Exception {
    if (!(o instanceof Serializable)) {
      System.err.println("Skipping non-serializable " + o.getClass().getName());
      return null;
    }

    return validateSerialization(o);
  }
  
  private Object validateSerialization(Object o) throws Exception {
    System.out.println("TESTING " + o.getClass());
    assertTrue(o instanceof Serializable);

    return deserialize(serialize(o));
  }
  
  private static byte[] serialize(Object obj) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(obj);
    oos.close();
    return baos.toByteArray();
  }
  
  private static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
    Object rv = ois.readObject();
    ois.close();
    return rv;
  }

}
