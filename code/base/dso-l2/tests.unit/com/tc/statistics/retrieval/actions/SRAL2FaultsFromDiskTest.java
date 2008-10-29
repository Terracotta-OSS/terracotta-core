/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.object.ObjectID;
import com.tc.objectserver.context.ManagedObjectFaultingContext;
import com.tc.objectserver.core.api.DSOGlobalServerStatsImpl;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.impl.TestServerConfigurationContext;
import com.tc.objectserver.handler.ManagedObjectFaultHandler;
import com.tc.objectserver.impl.TestManagedObjectStore;
import com.tc.objectserver.impl.TestObjectManager;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticType;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Random;

public class SRAL2FaultsFromDiskTest extends TCTestCase {

  static {
    TCProperties tcProps = TCPropertiesImpl.getProperties();
    tcProps.setProperty(TCPropertiesConsts.L2_OBJECTMANAGER_FAULT_LOGGING_ENABLED, "true");
    System.out.println("set to true");
  }

  private ManagedObjectFaultHandler managedObjectFaultHandler;
  private DSOGlobalServerStatsImpl  dsoGlobalServerStats;
  private static Random             random = new Random(System.currentTimeMillis());

  protected void setUp() throws Exception {
    final CounterManager counterManager = new CounterManagerImpl();
    final SampledCounterConfig sampledCounterConfig = new SampledCounterConfig(1, 10, true, 0L);
    final SampledCounter faultsFromDisk = (SampledCounter) counterManager.createCounter(sampledCounterConfig);
    final SampledCounter time2FaultCounter = (SampledCounter) counterManager.createCounter(sampledCounterConfig);
    final SampledCounter time2AddCounter = (SampledCounter) counterManager.createCounter(sampledCounterConfig);

    dsoGlobalServerStats = new DSOGlobalServerStatsImpl(null, null, null, null, null, null, faultsFromDisk,
                                                        time2FaultCounter, time2AddCounter);

    this.managedObjectFaultHandler = new ManagedObjectFaultHandler(faultsFromDisk, time2FaultCounter, time2AddCounter);
    final TestServerConfigurationContext serverConfigContext = new TestServerConfigurationContext();
    serverConfigContext.objectManager = new TestObjectManager() {
      public void addFaultedObject(final ObjectID oid, final ManagedObject mo, final boolean removeOnRelease) {
        ThreadUtil.reallySleep(100);
      }
    };
    serverConfigContext.objectStore = new TestManagedObjectStore();
    managedObjectFaultHandler.initialize(serverConfigContext);
  }

  protected void tearDown() throws Exception {
    this.managedObjectFaultHandler = null;
    this.dsoGlobalServerStats = null;
  }

  public void testL2FaultsFromDisk() throws InterruptedException {
    SRAL2FaultsFromDisk faultsFromDisk = new SRAL2FaultsFromDisk(dsoGlobalServerStats);
    Assert.assertEquals(StatisticType.SNAPSHOT, faultsFromDisk.getType());
    ManagedObjectFaultingContext context = getContext();

    for (int i = 1; i <= 30; i++) {
      System.out.println("Simulating object fault from disk...");
      managedObjectFaultHandler.handleEvent(context);
      StatisticData[] data = faultsFromDisk.retrieveStatisticData();
      Assert.assertEquals(3, data.length);
      assertData(data[0], SRAL2FaultsFromDisk.ELEMENT_NAME_FAULT_COUNT);
      assertData(data[1], SRAL2FaultsFromDisk.ELEMENT_NAME_TIME_2_FAULT_FROM_DISK);
      assertData(data[2], SRAL2FaultsFromDisk.ELEMENT_NAME_TIME_2_ADD_2_OBJ_MGR);
      Thread.sleep(100);
    }
  }

  private void assertData(final StatisticData statisticData, String element) {
    Assert.assertEquals(SRAL2FaultsFromDisk.ACTION_NAME, statisticData.getName());
    Assert.assertEquals(element, statisticData.getElement());
    Assert.assertNull(statisticData.getAgentIp());
    Assert.assertNull(statisticData.getAgentDifferentiator());
    Long count = (Long) statisticData.getData();
    Assert.assertTrue(count.longValue() >= 0); // Can't really assert on the value
    System.out.println("Asserted statistic data.");
  }

  private ManagedObjectFaultingContext getContext() {
    return new ManagedObjectFaultingContext(new ObjectID(random.nextLong()), true);
  }
}
