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
package com.tc.objectserver.entity;

import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import com.tc.async.api.StageManager;
import com.tc.entity.VoltronEntityMessage;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.objectserver.core.impl.ManagementTopologyEventCollector;
import com.tc.util.Assert;
import static com.tc.util.Assert.assertEquals;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.tc.util.Assert.assertEquals;


public class ClientEntityStateManagerImplTest {
  private ClientEntityStateManager clientEntityStateManager;
  private ManagementTopologyEventCollector collector;
  private Sink requestSink;
  private Stage requestStage;
  private StageManager stageManager;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    requestSink = mock(Sink.class);
    requestStage = mock(Stage.class);
    when(requestStage.getSink()).thenReturn(requestSink);
    stageManager = mock(StageManager.class);
    when(stageManager.getStage(any(), any())).thenReturn(requestStage);
    collector = mock(ManagementTopologyEventCollector.class);
    clientEntityStateManager = new ClientEntityStateManagerImpl();
    MessageChannel channel = mock(MessageChannel.class);
    when(channel.getRemoteNodeID()).thenReturn(new ClientID(1));
  }

  @Test
  public void testAddForNewClient() throws Exception {
    ClientDescriptorImpl cd = new ClientDescriptorImpl(new ClientID(1), new ClientInstanceID(1));
    assertTrue(addReference(cd, new EntityID("foo", "bar")));
  }

  @Test
  public void testAddTwice() throws Exception {
    ClientDescriptorImpl cd = new ClientDescriptorImpl(new ClientID(1), new ClientInstanceID(1));
    assertTrue(addReference(cd, new EntityID("foo", "bar")));
    assertFalse(addReference(cd, new EntityID("foo", "bar")));
  }

  @Test
  public void testRemoveUnknown() throws Exception {
    ClientDescriptorImpl cd = new ClientDescriptorImpl(new ClientID(1), new ClientInstanceID(1));
    try {
      assertFalse(removeReference(cd));
      Assert.fail();
    } catch (Throwable ee) {
      // expected
    }
  }

  @Test
  public void testChannelRemoved() throws Exception {
    EntityID entityID = new EntityID("foo", "bar");
    ClientInstanceID clientInstanceID = new ClientInstanceID(1);
    long version = 1;
    ClientID clientID = new ClientID(1);

    clientEntityStateManager.addReference(new ClientDescriptorImpl(clientID, clientInstanceID), entityID);
    List<VoltronEntityMessage> list = clientEntityStateManager.clientDisconnected(clientID);

    assertEquals(1, list.size());
    assertEquals(entityID, list.get(0).getEntityDescriptor().getEntityID());
  }

  @Test
  public void testVerifyNoReferences() throws Exception {
    // Verify that there are no references.
    assertTrue(verifyNoReferences(new EntityID("foo", "bar")));
    // Add a reference.
    ClientDescriptorImpl cd = new ClientDescriptorImpl(new ClientID(1), new ClientInstanceID(1));
    assertTrue(addReference(cd, new EntityID("foo", "bar")));
    // Verify that there now are references.
    assertFalse(verifyNoReferences(new EntityID("foo", "bar")));
    // Remove the reference.
    assertTrue(removeReference(cd));
    // Verify that there are no references.
    assertTrue(verifyNoReferences(new EntityID("foo", "bar")));
  }

  private boolean addReference(ClientDescriptorImpl clientID, EntityID descriptor) {
    // This only fails by asserting.
    boolean didSucceed = false;
    try {
      clientEntityStateManager.addReference(clientID, descriptor);
      didSucceed = true;
    } catch (AssertionError e) {
      didSucceed = false;
    }
    return didSucceed;
  }

  private boolean removeReference(ClientDescriptorImpl clientID) {
      return clientEntityStateManager.removeReference(clientID);
  }

  private boolean verifyNoReferences(EntityID eid) {
    return clientEntityStateManager.verifyNoReferences(eid);
  }
  
  private Matcher<Collection<EntityDescriptor>> collectionMatcher(Collection<EntityDescriptor> list) {
    return new BaseMatcher<Collection<EntityDescriptor>>() {
      @Override
      public boolean matches(Object item) {
        Collection<EntityDescriptor> c = (Collection)item;
        return list.size() == c.size() && list.containsAll(c);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Collection with (" + list + ")");
      }
    };
  }

  private Matcher<VoltronEntityMessage> hasClientAndEntityIDs(final ClientID clientID, final EntityID entityID) {
    return new BaseMatcher<VoltronEntityMessage>() {
      @Override
      public boolean matches(Object o) {
        if (o instanceof VoltronEntityMessage) {
          VoltronEntityMessage message = (VoltronEntityMessage) o;
          return message.getEntityDescriptor().getEntityID().equals(entityID) && message.getSource().equals(clientID);
        }
        return false;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("VoltronEntityMessage with (" + clientID + ", " + entityID + ")");
      }
    };
  }
}
