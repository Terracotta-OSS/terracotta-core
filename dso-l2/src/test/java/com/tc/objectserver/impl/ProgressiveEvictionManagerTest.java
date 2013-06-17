/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ResourceManager;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import com.tc.objectserver.managedobject.ManagedObjectStateStaticConfig;
import com.tc.objectserver.persistence.EvictionTransactionPersistor;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.util.ObjectIDSet;
import java.util.concurrent.Future;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import static org.mockito.Mockito.*;
import org.terracotta.corestorage.monitoring.MonitoredResource;


/**
 *
 * @author mscott
 */
public class ProgressiveEvictionManagerTest {
  
  ProgressiveEvictionManager mgr;
  
  public ProgressiveEvictionManagerTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
    PersistentManagedObjectStore store = mock(PersistentManagedObjectStore.class);
    ObjectIDSet set = new ObjectIDSet();
    for (int x=0;x<2048;x++) {
      set.add(new ObjectID((long)x));
    }
    when(store.getAllEvictableObjectIDs()).thenReturn(set);
    ObjectManager objectMgr = mock(ObjectManager.class);
    ManagedObject mo = mock(ManagedObject.class);
    EvictableMapState map = mock(EvictableMapState.class);
    when(mo.getManagedObjectState()).thenReturn(map);
    when(map.getSize()).thenReturn(0);
    when(map.getType()).thenReturn(ManagedObjectStateStaticConfig.SERVER_MAP.getStateObjectType());
    when(map.getCacheName()).thenReturn("TESTMAP");
    when(map.startEviction()).thenReturn(false);
    when(map.getClassName()).thenReturn("TESTMAPCLASS");
    when(objectMgr.getObjectByIDReadOnly(Matchers.<ObjectID>any())).thenReturn(mo);
    mgr = new ProgressiveEvictionManager(objectMgr, mock(MonitoredResource.class), store, 
            mock(ClientObjectReferenceSet.class), mock(ServerTransactionFactory.class), new TCThreadGroup(mock(ThrowableHandler.class)), 
            mock(ResourceManager.class), mock(CounterManager.class), mock(EvictionTransactionPersistor.class), false);
  }
  
  @After
  public void tearDown() {
  }

  @Test
  public void testEmergencyEviction() throws Throwable {
    Future<SampledRateCounter> counter = mgr.emergencyEviction(5);
    counter.cancel(false);
    mgr.shutdownEvictor();
    System.out.println(mgr.getCurrentlyEvicting());
    Assert.assertTrue(mgr.getCurrentlyEvicting().isEmpty());
  }
}
