/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

import com.tc.services.EmptyServiceProviderConfiguration;
import org.terracotta.entity.Service;

import com.tc.test.TCTestCase;

import java.io.IOException;
import java.util.Collection;

import static org.mockito.Mockito.mock;
import org.terracotta.persistence.IPersistentStorage;
import org.terracotta.persistence.KeyValueStorage;


/**
 * VERY simple tests of FlatFileStorageServiceProvider and some of what it produces.
 */
public class FlatFileStorageServiceProviderTest extends TCTestCase {
  private FlatFileStorageServiceProvider provider;
  
  @Override
  public void setUp() {
    provider = new FlatFileStorageServiceProvider();
    provider.initialize(new EmptyServiceProviderConfiguration(FlatFileStorageServiceProvider.class));
  }

  public void testServiceType() {
    Collection<Class<?>> serviceTypes = provider.getProvidedServiceTypes();
    assertEquals(1, serviceTypes.size());
    assertTrue(serviceTypes.contains(IPersistentStorage.class));
  }
  
  public void testGetService() throws IOException {
    long consumerID = 1;
    PersistentStorageServiceConfiguration configuration = mock(PersistentStorageServiceConfiguration.class);
    Service<IPersistentStorage> service = provider.getService(consumerID, configuration);
    assertTrue(service instanceof FlatFileStorageService);
    IPersistentStorage storage = service.get();
    storage.create();
    KeyValueStorage<Integer, String> keyValueStorage = storage.getKeyValueStorage("numbers", Integer.class, String.class);
    keyValueStorage.put(2, "two");
    keyValueStorage.put(3, "three");
    keyValueStorage.put(1, "one");
    assertEquals("three", keyValueStorage.get(3));
    service.destroy();
    
    // Reload it to see if the data is still there.
    service = provider.getService(consumerID, configuration);
    storage = service.get();
    storage.open();
    keyValueStorage = storage.getKeyValueStorage("numbers", Integer.class, String.class);
    assertEquals("one", keyValueStorage.get(1));
    assertEquals("two", keyValueStorage.get(2));
    assertEquals("three", keyValueStorage.get(3));
  }
  
  public void testCreateReplacesData() throws IOException {
    long consumerID = 1;
    PersistentStorageServiceConfiguration configuration = mock(PersistentStorageServiceConfiguration.class);
    Service<IPersistentStorage> service = provider.getService(consumerID, configuration);
    assertTrue(service instanceof FlatFileStorageService);
    IPersistentStorage storage = service.get();
    storage.create();
    KeyValueStorage<Integer, String> keyValueStorage = storage.getKeyValueStorage("numbers", Integer.class, String.class);
    keyValueStorage.put(1, "one");
    assertEquals("one", keyValueStorage.get(1));
    service.destroy();
    
    // Recreate the storage and verify that the data is NOT there.
    service = provider.getService(consumerID, configuration);
    storage = service.get();
    storage.create();
    keyValueStorage = storage.getKeyValueStorage("numbers", Integer.class, String.class);
    assertEquals(null, keyValueStorage.get(1));
  }
}
