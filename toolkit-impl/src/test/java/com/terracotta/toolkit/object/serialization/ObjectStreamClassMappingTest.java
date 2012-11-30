/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object.serialization;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.tc.platform.PlatformService;

import java.io.IOException;
import java.io.ObjectStreamClass;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import junit.framework.Assert;

public class ObjectStreamClassMappingTest {

  private ObjectStreamClassMapping serializer;
  private SerializerMap  localSerializerMap;

  @Before
  public void init() {
    localSerializerMap = new LocalSerializerMap();
    serializer = new ObjectStreamClassMapping(Mockito.mock(PlatformService.class), localSerializerMap);
  }

  @Test
  public void testSerializer() throws IOException, ClassNotFoundException {
    Set<Class> classSet = new HashSet<Class>();
    populateSet(classSet);

    HashMap<Integer, ObjectStreamClass> mappings = new HashMap<Integer, ObjectStreamClass>();

    for (Class cl : classSet) {
      ObjectStreamClass osc = ObjectStreamClass.lookup(cl);
      mappings.put(serializer.getMappingFor(osc), osc);
    }

    for (Entry<Integer, ObjectStreamClass> entry : mappings.entrySet()) {
      Assert.assertEquals(entry.getValue().toString(), serializer.getObjectStreamClassFor(entry.getKey()).toString());
    }
  }

  private void populateSet(Set<Class> classSet) {
    classSet.add(Integer.class);
    classSet.add(Long.class);
    classSet.add(Character.class);
    classSet.add(Float.class);
    classSet.add(Double.class);
    classSet.add(Byte.class);
    classSet.add(Boolean.class);
    classSet.add(Short.class);
    classSet.add(String.class);
    classSet.add(Long.class);
    classSet.add(Enum.class);
  }

  private static class LocalSerializerMap<K, V> implements SerializerMap<K, V> {
    private final Map<K, V> localHashMap = new HashMap<K, V>();

    @Override
    public V put(K key, V value) {
      return localHashMap.put(key, value);
    }

    @Override
    public V get(K key) {
      return localHashMap.get(key);
    }

    @Override
    public V localGet(K key) {
      return get(key);
    }

  }

}
