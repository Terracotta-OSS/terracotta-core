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
import com.tc.object.FetchID;
import com.tc.util.Assert;
import static com.tc.util.Assert.assertEquals;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ClientEntityStateManagerImplTest {
  private ClientEntityStateManager clientEntityStateManager;
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
    clientEntityStateManager = new ClientEntityStateManagerImpl();
    MessageChannel channel = mock(MessageChannel.class);
    when(channel.getRemoteNodeID()).thenReturn(new ClientID(1));
  }

  @Test
  public void testAddForNewClient() throws Exception {
    ClientDescriptorImpl cd = new ClientDescriptorImpl(new ClientID(1), new ClientInstanceID(1));
    assertTrue(addReference(cd, new FetchID(1)));
  }

  @Test
  public void testAddTwice() throws Exception {
    ClientDescriptorImpl cd = new ClientDescriptorImpl(new ClientID(1), new ClientInstanceID(1));
    assertTrue(addReference(cd, new FetchID(1)));
    assertFalse(addReference(cd, new FetchID(1)));
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
    FetchID entityID = new FetchID(1);
    ClientInstanceID clientInstanceID = new ClientInstanceID(1);
    ClientID clientID = new ClientID(1);

    clientEntityStateManager.addReference(new ClientDescriptorImpl(clientID, clientInstanceID), entityID);
    List<FetchID> list = clientEntityStateManager.clientDisconnected(clientID);

    assertEquals(1, list.size());
    assertEquals(entityID, list.get(0));
  }

  @Test
  public void testVerifyNoReferences() throws Exception {
    // Verify that there are no references.
    assertTrue(verifyNoReferences(new FetchID(1)));
    // Add a reference.
    ClientDescriptorImpl cd = new ClientDescriptorImpl(new ClientID(1), new ClientInstanceID(1));
    assertTrue(addReference(cd, new FetchID(1)));
    // Verify that there now are references.
    assertFalse(verifyNoReferences(new FetchID(1)));
    // Remove the reference.
    assertTrue(removeReference(cd));
    // Verify that there are no references.
    assertTrue(verifyNoReferences(new FetchID(1)));
  }

  private boolean addReference(ClientDescriptorImpl clientID, FetchID descriptor) {
    // This only fails by asserting.
    boolean didSucceed = false;
    try {
      didSucceed = clientEntityStateManager.addReference(clientID, descriptor);
    } catch (AssertionError e) {
      didSucceed = false;
    }
    return didSucceed;
  }

  private boolean removeReference(ClientDescriptorImpl clientID) {
      return clientEntityStateManager.removeReference(clientID);
  }

  private boolean verifyNoReferences(FetchID eid) {
    return clientEntityStateManager.verifyNoEntityReferences(eid);
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
