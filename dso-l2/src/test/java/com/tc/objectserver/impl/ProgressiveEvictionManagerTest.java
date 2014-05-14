/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.net.groups.GroupManager;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableEntry;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ResourceManager;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import com.tc.objectserver.managedobject.ManagedObjectStateStaticConfig;
import com.tc.objectserver.persistence.EvictionTransactionPersistor;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionBatchManager;
import com.tc.stats.counter.CounterConfig;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;
import java.util.Collections;
import java.util.Map;
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
  EvictableMapState map;
  ObjectManager objectMgr;
  
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
    ObjectIDSet set = new BitSetObjectIDSet();
    for (int x=0;x<2048;x++) {
      set.add(new ObjectID((long)x));
    }
    when(store.getAllEvictableObjectIDs()).thenReturn(set);
    objectMgr = mock(ObjectManager.class);
    ManagedObject mo = mock(ManagedObject.class);
    map = mock(EvictableMapState.class);
    when(mo.getManagedObjectState()).thenReturn(map);
    when(map.getSize()).thenReturn(0);
    when(map.getType()).thenReturn(ManagedObjectStateStaticConfig.SERVER_MAP.getStateObjectType());
    when(map.getCacheName()).thenReturn("TESTMAP");
    when(map.startEviction()).thenReturn(false);
    when(map.getClassName()).thenReturn("TESTMAPCLASS");
    when(objectMgr.getObjectByIDReadOnly(Matchers.<ObjectID>any())).thenReturn(mo);
    
    CounterManager counter = mock(CounterManager.class);
    when(counter.createCounter(Matchers.<CounterConfig>any())).thenReturn(mock(SampledRateCounter.class));
    mgr = new ProgressiveEvictionManager(objectMgr, Collections.singletonList(mock(MonitoredResource.class)), store, 
            mock(ClientObjectReferenceSet.class), mock(ServerTransactionFactory.class), new TCThreadGroup(mock(ThrowableHandler.class)), 
            mock(ResourceManager.class), counter, mock(EvictionTransactionPersistor.class), false, false);
    
    ServerConfigurationContext cxt = mock(ServerConfigurationContext.class);
    Stage stage = mock(Stage.class);
    when(stage.getSink()).thenReturn(mock(Sink.class));
    when(cxt.getStage(Matchers.anyString())).thenReturn(stage);
    when(cxt.getTransactionBatchManager()).thenReturn(mock(TransactionBatchManager.class));
    when(cxt.getTransactionManager()).thenReturn(mock(ServerTransactionManager.class));
    L2Coordinator l2 = mock(L2Coordinator.class);
    when(l2.getGroupManager()).thenReturn(mock(GroupManager.class));
    when(cxt.getL2Coordinator()).thenReturn(l2);
    mgr.initializeContext(cxt);
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
  
  @Test
  public void testCapacityEviction() throws Throwable {
    when(map.startEviction()).thenReturn(true);
    when(map.isEvictionEnabled()).thenReturn(true);
    when(map.getSize()).thenReturn(1000);
    when(map.getMaxTotalCount()).thenReturn(500);
    Map<Object,EvictableEntry> collectionMap = mock(Map.class);
    when(map.getRandomSamples(Matchers.anyInt(),Matchers.<ClientObjectReferenceSet>any(),Matchers.<SamplingType>any())).thenReturn(mock(Map.class));
    Assert.assertTrue(mgr.scheduleCapacityEviction(new ObjectID(1)));
    mgr.shutdownEvictor();
//    verify(map).evictionCompleted();
    verify(map,atLeastOnce()).getMaxTotalCount();
    verify(map,atLeastOnce()).getSize();
    verify(map).getRandomSamples(Matchers.eq(500), Matchers.<ClientObjectReferenceSet>any(), Matchers.<SamplingType>any());
    verify(map).isEvictionEnabled();
    verify(map).startEviction();
    verify(objectMgr).releaseReadOnly(Matchers.<ManagedObject>any());
    verify(objectMgr).getObjectByIDReadOnly(Matchers.<ObjectID>any());    
  }
  
  @Test
  public void testDoEvictionOn() throws Throwable {
    CapacityEvictionTrigger trigger = new CapacityEvictionTrigger(mgr, new ObjectID(1));
    try {
      mgr.doEvictionOn(trigger);
    } catch ( AssertionError err ) {
      System.err.println("expected assertion error");
    }
    ServerMapEvictionEngine engine = mgr.getEngine();
    engine.markEvictionInProgress(trigger.getId());
    Assert.assertTrue(mgr.doEvictionOn(trigger));
    mgr.shutdownEvictor();
    Assert.assertTrue(mgr.getCurrentlyEvicting().isEmpty());
    verify(objectMgr).getObjectByIDReadOnly(new ObjectID(1));
    verify(objectMgr).releaseReadOnly(Matchers.<ManagedObject>any());
  }
}
