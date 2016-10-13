/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.persistence;

import com.tc.test.TCTestCase;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.hamcrest.Matchers;
import org.junit.Assert;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.terracotta.persistence.IPersistentStorage;
import org.terracotta.persistence.KeyValueStorage;


/**
 * VERY simple tests of FlatFileStorageServiceProvider and some of what it produces.
 */
public class FlatFileStorageServiceProviderTest extends TCTestCase {
  private FlatFileStorageServiceProvider provider;
  
  @Override
  public void setUp() throws Exception {
    provider = new FlatFileStorageServiceProvider();
    provider.initialize(new FlatFileStorageProviderConfiguration(getTempDirectory()));
  }

  public void testServiceType() {
    Collection<Class<?>> serviceTypes = provider.getProvidedServiceTypes();
    assertEquals(1, serviceTypes.size());
    assertTrue(serviceTypes.contains(IPersistentStorage.class));
  }
  
  public void testGetService() throws IOException {
    long consumerID = 1;
    PersistentStorageServiceConfiguration configuration = mock(PersistentStorageServiceConfiguration.class);
    when(configuration.getServiceType()).thenReturn(IPersistentStorage.class);
    IPersistentStorage storage = provider.getService(consumerID, configuration);
    assertTrue(storage instanceof FlatFilePersistentStorage);
    storage.create();
    KeyValueStorage<Integer, String> keyValueStorage = storage.getKeyValueStorage("numbers", Integer.class, String.class);
    keyValueStorage.put(2, "two");
    keyValueStorage.put(3, "three");
    keyValueStorage.put(1, "one");
    assertEquals("three", keyValueStorage.get(3));
    storage.close();
    
    // Reload it to see if the data is still there.
    storage = provider.getService(consumerID, configuration);
    storage.open();
    keyValueStorage = storage.getKeyValueStorage("numbers", Integer.class, String.class);
    assertEquals("one", keyValueStorage.get(1));
    assertEquals("two", keyValueStorage.get(2));
    assertEquals("three", keyValueStorage.get(3));
  }
  
  public void testCreateReplacesData() throws IOException {
    long consumerID = 1;
    PersistentStorageServiceConfiguration configuration = mock(PersistentStorageServiceConfiguration.class);
    when(configuration.getServiceType()).thenReturn(IPersistentStorage.class);
    IPersistentStorage storage = provider.getService(consumerID, configuration);
    assertTrue(storage instanceof FlatFilePersistentStorage);
    storage.create();
    KeyValueStorage<Integer, String> keyValueStorage = storage.getKeyValueStorage("numbers", Integer.class, String.class);
    keyValueStorage.put(1, "one");
    assertEquals("one", keyValueStorage.get(1));
    storage.close();
    
    // Recreate the storage and verify that the data is NOT there.
    storage = provider.getService(consumerID, configuration);
    storage.create();
    keyValueStorage = storage.getKeyValueStorage("numbers", Integer.class, String.class);
    assertEquals(null, keyValueStorage.get(1));
  }
  
  @SuppressWarnings("deprecation")
  public void testMultiThreaded() throws Exception {
//  very dumb random test that makes sure we can do things multi-threaded without blowing up.
    long consumerID = 1;
    PersistentStorageServiceConfiguration configuration = mock(PersistentStorageServiceConfiguration.class);
    when(configuration.getServiceType()).thenReturn(IPersistentStorage.class);
    IPersistentStorage storage = provider.getService(consumerID, configuration);
    assertTrue(storage instanceof FlatFilePersistentStorage);
    storage.create();
    final KeyValueStorage<Integer, Integer> keyValueStorage = storage.getKeyValueStorage("numbers", Integer.class, Integer.class);
    ExecutorService e = Executors.newFixedThreadPool(5);
    Runnable mutator = ()-> {
      int next = 0;
      while (next < 1000) {
        keyValueStorage.put(((int)(Math.random() * 100)) % 10, next++);
      }
    };
    AtomicBoolean bool = new AtomicBoolean(true);
    Callable<Integer> reader = () -> {
      int count = 0;
      while (bool.get()) {
        keyValueStorage.get(((int)(Math.random() * 100)) % 10);
        count++;
      }
      return count;
    };
    
    Future<Integer> r = e.submit(reader);
    Future<?> w = e.submit(mutator);
    Future<?> w1 = e.submit(mutator);
    Future<?> w2 = e.submit(mutator);
    Future<?> w3 = e.submit(mutator);
    
    w.get();
    w1.get();
    w2.get();
    w3.get();
    bool.set(false);
    int ec = r.get();
    Assert.assertThat(ec, Matchers.greaterThan(0));
    // Recreate the storage and verify that the data is NOT there.
    storage = provider.getService(consumerID, configuration);
    storage.open();
    assertNotNull(storage.getKeyValueStorage("numbers", Integer.class, Integer.class).get(((int)(Math.random() * 100)) % 10));
    assertEquals(storage.getKeyValueStorage("numbers", Integer.class, Integer.class).size(), 10);
  }
}
