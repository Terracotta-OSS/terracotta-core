/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.util.ResourceBundle;

public class TerracottaOperatorEventResourcesTest extends TCTestCase {
  private ResourceBundle resources;

  @Override
  protected void setUp() throws Exception {
    this.resources = ResourceBundle.getBundle(getClass().getPackage().getName() + ".messages");
  }

  public void testResources() {
    /**
     * Memeory manager messages
     */
    Assert.assertTrue(TerracottaOperatorEventResources.getLongGCMessage().equals(this.resources.getObject("long.gc")));
    Assert.assertTrue(TerracottaOperatorEventResources.getLongGCAndOffheapRecommendationMessage()
        .equals(this.resources.getObject("long.gc.no.offheap")));
    Assert.assertTrue(TerracottaOperatorEventResources.getHighMemoryUsageMessage()
        .equals(this.resources.getObject("high.memory.usage")));

    /**
     * DGC messages
     */
    Assert.assertTrue(TerracottaOperatorEventResources.getDGCStartedMessage().equals(this.resources
                                                                                         .getObject("dgc.started")));
    Assert.assertTrue(TerracottaOperatorEventResources.getDGCFinishedMessage().equals(this.resources
                                                                                          .getObject("dgc.finished")));
    Assert.assertTrue(TerracottaOperatorEventResources.getDGCCanceledMessage().equals(this.resources
                                                                                          .getObject("dgc.canceled")));

    /**
     * HA Messages
     */
    Assert.assertTrue(TerracottaOperatorEventResources.getNodeAvailabiltyMessage()
        .equals(this.resources.getObject("node.availability")));
    Assert.assertTrue(TerracottaOperatorEventResources.getOOODisconnectMessage()
        .equals(this.resources.getObject("ooo.disconnect")));
    Assert.assertTrue(TerracottaOperatorEventResources.getOOOConnectMessage().equals(this.resources
                                                                                         .getObject("ooo.connect")));
    Assert.assertTrue(TerracottaOperatorEventResources.getClusterNodeStateChangedMessage()
        .equals(this.resources.getObject("node.state")));

    Assert.assertTrue(TerracottaOperatorEventResources.getHandshakeRejectedMessage()
        .equals(this.resources.getObject("handshake.reject")));

    /**
     * Zap Messagse
     */
    Assert.assertTrue(TerracottaOperatorEventResources.getZapRequestReceivedMessage()
        .equals(this.resources.getObject("zap.received")));
    Assert.assertTrue(TerracottaOperatorEventResources.getZapRequestAcceptedMessage()
        .equals(this.resources.getObject("zap.accepted")));
    Assert
        .assertTrue(TerracottaOperatorEventResources.getDirtyDBMessage().equals(this.resources.getObject("dirty.db")));

    /**
     * Off heap message
     */
    Assert.assertTrue(TerracottaOperatorEventResources.getOffHeapMemoryUsageMessage()
        .equals(this.resources.getObject("offheap.memory.usage")));
    Assert.assertTrue(TerracottaOperatorEventResources.getOffHeapMemoryEvictionMessage()
        .equals(this.resources.getObject("offheap.memory.eviction")));
    Assert.assertTrue(TerracottaOperatorEventResources.getOffHeapObjectCachedMessage()
        .equals(this.resources.getObject("offheap.memory.objectCached")));

  }
}
