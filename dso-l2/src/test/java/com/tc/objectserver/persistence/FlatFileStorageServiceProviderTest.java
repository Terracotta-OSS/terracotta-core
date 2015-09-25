/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

import com.tc.test.TCTestCase;

import java.io.IOException;
import java.util.Collection;

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
}
