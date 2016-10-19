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

import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import org.junit.Before;
import org.junit.Test;

import com.tc.object.EntityID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.core.api.ITopologyEventCollector;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.testentity.TestEntity;
import com.tc.services.InternalServiceRegistry;
import com.tc.services.TerracottaServiceProviderRegistry;
import com.tc.util.Assert;
import java.util.Collections;
import java.util.Optional;

import static java.util.Optional.empty;
import java.util.Set;
import java.util.function.BiConsumer;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.mockito.Matchers;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.terracotta.entity.ClientDescriptor;


public class EntityManagerImplTest {
  private EntityManager entityManager;
  private EntityID id;
  private long version;
  private long consumerID;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    TerracottaServiceProviderRegistry registry = mock(TerracottaServiceProviderRegistry.class);
    when(registry.subRegistry(any(Long.class))).thenReturn(mock(InternalServiceRegistry.class));
    RequestProcessor processor = mock(RequestProcessor.class);
    when(processor.scheduleRequest(any(), any(), any(), any(), Matchers.anyBoolean(), Matchers.anyInt())).then((invoke)->{
        ((Runnable)invoke.getArguments()[3]).run();
        return null;
      });
    entityManager = new EntityManagerImpl(
        registry,
        mock(ClientEntityStateManager.class),
        mock(ITopologyEventCollector.class),
        mock(RequestProcessor.class),
        mock(BiConsumer.class)
    );
    id = new EntityID(TestEntity.class.getName(), "foo");
    version = 1;
  }

  @Test
  public void testGetNonExistent() throws Exception {
    assertThat(entityManager.getEntity(id, version), is(empty()));
  }

  @Test
  public void testCreateEntity() throws Exception {
    entityManager.createEntity(id, version, consumerID, true);
    assertThat(entityManager.getEntity(id, version).get().getID(), is(id));
  }

  @Test
  public void testCreateExistingEntity() throws Exception {
    ManagedEntity entity = entityManager.createEntity(id, version, consumerID, true);
    ManagedEntity second = entityManager.createEntity(id, version, consumerID, true);
    Assert.assertEquals(entity, second);
  }
  
  @Test
  public void testNullEntityChecks() throws Exception {
    Optional<ManagedEntity> check = entityManager.getEntity(EntityID.NULL_ID, 0);
    Assert.assertFalse(check.isPresent());
    ManagedEntity entity = entityManager.createEntity(id, version, consumerID, true);
    check = entityManager.getEntity(id, version);
    Assert.assertTrue(check.isPresent());
    check = entityManager.getEntity(id, 0);
    //  make sure zero versions go to empty
    Assert.assertFalse(check.isPresent());
    //  make sure null goes to empty
    check = entityManager.getEntity(EntityID.NULL_ID, 1);
    Assert.assertFalse(check.isPresent());
  }

  @Test
  public void testDestroyEntity() throws Exception {
    entityManager.enterActiveState();
    ManagedEntity entity = entityManager.createEntity(id, version, consumerID, true);
    Thread.currentThread().setName(ServerConfigurationContext.VOLTRON_MESSAGE_STAGE);
    ServerEntityRequest req = new ServerEntityRequest() {
      @Override
      public ServerEntityAction getAction() {
        return ServerEntityAction.DESTROY_ENTITY;
      }

      @Override
      public ClientID getNodeID() {
        return ClientID.NULL_ID;
      }

      @Override
      public TransactionID getTransaction() {
        return TransactionID.NULL_ID;
      }

      @Override
      public TransactionID getOldestTransactionOnClient() {
        return TransactionID.NULL_ID;
      }

      @Override
      public ClientDescriptor getSourceDescriptor() {
        return new ClientDescriptorImpl(ClientID.NULL_ID, new EntityDescriptor(id, ClientInstanceID.NULL_ID, version));
      }

      @Override
      public Set<NodeID> replicateTo(Set<NodeID> passives) {
        return Collections.emptySet();
      }
    };
    //  set the destroyed flag in the entity
    entity.addRequestMessage(req, MessagePayload.EMPTY, null, null);
    //  remove it from the manager
    entityManager.removeDestroyed(id);
    assertThat(entityManager.getEntity(id, version), is(empty()));
  }

}
