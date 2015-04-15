/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.operatorevent;

import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import org.junit.Test;

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
     * Inline DGC
     */
    Assert.assertTrue(TerracottaOperatorEventResources.getInlineDGCReferenceCleanupStartedMessage()
        .equals(this.resources.getObject("inlineDgc.cleanup.started")));
    Assert.assertTrue(TerracottaOperatorEventResources.getInlineDGCReferenceCleanupFinishedMessage()
        .equals(this.resources.getObject("inlineDgc.cleanup.finished")));
    Assert.assertTrue(TerracottaOperatorEventResources.getInlineDGCReferenceCleanupCanceledMessage()
        .equals(this.resources.getObject("inlineDgc.cleanup.canceled")));

    /**
     * HA Messages
     */
    Assert.assertTrue(TerracottaOperatorEventResources.getNodeAvailabiltyMessage()
        .equals(this.resources.getObject("node.availability")));
    Assert.assertTrue(TerracottaOperatorEventResources.getClusterNodeStateChangedMessage()
        .equals(this.resources.getObject("node.state")));

    Assert.assertTrue(TerracottaOperatorEventResources.getHandshakeRejectedMessage()
        .equals(this.resources.getObject("handshake.reject")));

    Assert.assertTrue(TerracottaOperatorEventResources.getActiveServerDisconnectMessage()
        .equals(this.resources.getObject("active.server.disconnect")));

    Assert.assertTrue(TerracottaOperatorEventResources.getMirrorServerDisconnectMessage()
        .equals(this.resources.getObject("mirror.server.disconnect")));
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
     * Dirty DB
     */
    Assert
        .assertTrue(TerracottaOperatorEventResources.getDirtyDBMessage().equals(this.resources.getObject("dirty.db")));

    /**
     * Servermap
     */
    Assert.assertTrue(TerracottaOperatorEventResources.getServerMapEvictionMessage()
        .equals(this.resources.getObject("servermap.eviction")));

    /**
     * config related message
     */
    Assert.assertTrue(TerracottaOperatorEventResources.getTimeDifferentMessage()
        .equals(this.resources.getObject("time.different")));
    Assert.assertTrue(TerracottaOperatorEventResources.getConfigReloadedMessage()
        .equals(this.resources.getObject("config.reloaded")));

    /**
     * resource management
     */
    Assert.assertTrue(TerracottaOperatorEventResources.getNearResourceCapacityLimit()
        .equals(this.resources.getObject("resource.nearcapacity")));
    Assert.assertTrue(TerracottaOperatorEventResources.getFullResourceCapacityLimit()
        .equals(this.resources.getObject("resource.fullcapacity")));
    Assert.assertTrue(TerracottaOperatorEventResources.getRestoredNormalResourceCapacity()
        .equals(this.resources.getObject("resource.capacityrestored")));
  }

  @Test
  public void testZapRequestReceivedEventEventType() {
      TerracottaOperatorEvent event = TerracottaOperatorEventFactory.createZapRequestReceivedEvent(null);
      Assert.assertEquals(event.getEventLevel(), TerracottaOperatorEvent.EventLevel.WARN);
  }

  @Test
  public void testZapRequestAcceptedEventEventType() {
      TerracottaOperatorEvent event = TerracottaOperatorEventFactory.createZapRequestAcceptedEvent(null);
      Assert.assertEquals(event.getEventLevel(), TerracottaOperatorEvent.EventLevel.WARN);
  }
}
