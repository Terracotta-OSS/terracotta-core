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

package com.terracotta.connection.entity;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.terracotta.connection.entity.Entity;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;

import com.tc.object.ClientEntityManager;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityID;
import com.tc.util.Assert;

import java.util.concurrent.atomic.AtomicLong;
import org.mockito.Mockito;


public class TerracottaEntityRefTest {
  @Test
  /**
   * A VERY simple test to check that we can fetch an object and that the expected paths are called.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void testFetch() throws Exception {
    // Set up the mocked infrastructure.
    ClientEntityManager mockClientEntityManager = mock(ClientEntityManager.class);
    EntityClientService<Entity, Void, ? extends EntityMessage, ? extends EntityResponse, Void> mockEntityClientService = mock(EntityClientService.class);
    Entity testEntity = mock(Entity.class);
    when(mockEntityClientService.create(any(EntityClientEndpoint.class), any(Void.class))).thenReturn(testEntity);
    EntityClientEndpoint mockTestEntityClientEndpoint = mock(EntityClientEndpoint.class);
    when(mockClientEntityManager.fetchEntity(any(EntityID.class), anyLong(), any(ClientInstanceID.class), any(MessageCodec.class), any(Runnable.class))).thenReturn(mockTestEntityClientEndpoint);
    
    // Now, run the test.
    long version = 1;
// clientids start at 1
    TerracottaEntityRef<Entity, Void, Void> testRef = new TerracottaEntityRef(mockClientEntityManager, Entity.class, version, "TEST", mockEntityClientService, new AtomicLong(1));
    Entity entity1 = testRef.fetchEntity(null);
    verify(mockClientEntityManager).fetchEntity(eq(new EntityID(Entity.class.getName(), "TEST")), anyLong(), any(ClientInstanceID.class), any(MessageCodec.class), any(Runnable.class));
    Assert.assertNotNull(entity1);
    Assert.assertEquals(testEntity, entity1);
    entity1.close();
    // Note that we don't see the corresponding readUnlockEntity call since it is called by the EntityClientEndpoint, when closed, but that is just a mock.
  }

  @Test
  /**
   * Test that tryDestroy interacts with the underlying systems as expected when it SUCCEEDED in getting the lock.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void testTryDestroySuccess() throws Exception {
    // Set up the mocked infrastructure.
    ClientEntityManager mockClientEntityManager = mock(ClientEntityManager.class);
    when(mockClientEntityManager.destroyEntity(any(EntityID.class), any(Long.class))).thenReturn(true);
    EntityClientService<Entity, Void, ? extends EntityMessage, ? extends EntityResponse, Void> mockEntityClientService = mock(EntityClientService.class);
    
    // Now, run the test.
    long version = 1;
// clientids start at 1
    TerracottaEntityRef<Entity, Void, Void> testRef = new TerracottaEntityRef(mockClientEntityManager, Entity.class, version, "TEST", mockEntityClientService, new AtomicLong(1));
    // We are going to delete this, directly.
    boolean didDestroy = testRef.destroy();
    Assert.assertTrue(didDestroy);
  }

  @Test
  /**
   * Test that tryDestroy interacts with the underlying systems as expected when it FAILED in getting the lock.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void testTryDestroyFailure() throws Exception {
    // Set up the mocked infrastructure.
    ClientEntityManager mockClientEntityManager = mock(ClientEntityManager.class);
    when(mockClientEntityManager.destroyEntity(Mockito.any(EntityID.class), Mockito.anyLong())).thenReturn(Boolean.FALSE);
    EntityClientService<Entity, Void, ? extends EntityMessage, ? extends EntityResponse, Void> mockEntityClientService = mock(EntityClientService.class);
    
    // Now, run the test.
    long version = 1;
// clientids start at 1
    TerracottaEntityRef<Entity, Void, Void> testRef = new TerracottaEntityRef(mockClientEntityManager, Entity.class, version, "TEST", mockEntityClientService, new AtomicLong(1));
    // We are going to delete this, directly.
    boolean didDestroy = testRef.destroy();
    Assert.assertFalse(didDestroy);
    // We should never have asked for the destroy to happen.
    verify(mockClientEntityManager).destroyEntity(any(EntityID.class), any(Long.class));
  }
}
