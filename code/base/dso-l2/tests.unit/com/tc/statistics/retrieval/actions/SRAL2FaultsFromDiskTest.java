/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.object.ObjectID;
import com.tc.objectserver.context.ManagedObjectFaultingContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.impl.TestServerConfigurationContext;
import com.tc.objectserver.handler.ManagedObjectFaultHandler;
import com.tc.objectserver.impl.TestManagedObjectStore;
import com.tc.objectserver.impl.TestObjectManager;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.properties.TCPropertiesConsts;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticType;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.util.Random;

public class SRAL2FaultsFromDiskTest extends TCTestCase {

  static {
    TCProperties tcProps = TCPropertiesImpl.getProperties();
    tcProps.setProperty(TCPropertiesConsts.L2_OBJECTMANAGER_FAULT_LOGGING_ENABLED, "true");
    System.out.println("set to true");
  }

  private ManagedObjectFaultHandler managedObjectFaultHandler;
  private static Random random = new Random(System.currentTimeMillis());

  protected void setUp() throws Exception {
    this.managedObjectFaultHandler = new ManagedObjectFaultHandler();
    final TestServerConfigurationContext serverConfigContext = new TestServerConfigurationContext();
    serverConfigContext.objectManager = new TestObjectManager() {
      public void addFaultedObject(final ObjectID oid, final ManagedObject mo, final boolean removeOnRelease) {
        //
      }
    };
    serverConfigContext.objectStore = new TestManagedObjectStore();
    managedObjectFaultHandler.initialize(serverConfigContext);
  }

  protected void tearDown() throws Exception {
    this.managedObjectFaultHandler = null;
  }

  public void testL2FaultsFromDisk() throws InterruptedException {
    SRAL2FaultsFromDisk faultsFromDisk = new SRAL2FaultsFromDisk(managedObjectFaultHandler);
    Assert.assertEquals(StatisticType.SNAPSHOT, faultsFromDisk.getType());
    ManagedObjectFaultingContext context = getContext();

    for (int i = 1; i <= 30; i++) {
      System.out.println("Simulating object fault from disk...");
      managedObjectFaultHandler.handleEvent(context);
      StatisticData[] data = faultsFromDisk.retrieveStatisticData();
      Assert.assertEquals(1, data.length);
      assertData(data[0], i);
      Thread.sleep(100);
    }
  }

  private void assertData(final StatisticData statisticData, final int expectedObjectCount) {
    Assert.assertEquals(SRAL2FaultsFromDisk.ACTION_NAME, statisticData.getName());
    Assert.assertNull(statisticData.getAgentIp());
    Assert.assertNull(statisticData.getAgentDifferentiator());
    Long count = (Long)statisticData.getData();
    Assert.assertEquals(expectedObjectCount, count.longValue());
    System.out.println("Asserted statistic data.");
  }

  private ManagedObjectFaultingContext getContext() {
    return new ManagedObjectFaultingContext(new ObjectID(random.nextLong()), true);
  }
}
