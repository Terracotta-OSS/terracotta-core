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
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.terracotta.connection.entity.Entity;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityClientService;

import com.tc.object.ClientEntityManager;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.util.Assert;
import java.util.concurrent.atomic.AtomicLong;


public class TerracottaEntityRefTest {
  @Test
  /**
   * A VERY simple test to check that we can fetch an object and that the expected paths are called.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void testFetch() throws Exception {
    // Set up the mocked infrastructure.
    ClientEntityManager mockClientEntityManager = mock(ClientEntityManager.class);
    MaintenanceModeService mockMaintenanceModeService = mock(MaintenanceModeService.class);
    EntityClientService<Entity, Void> mockEntityClientService = mock(EntityClientService.class);
    Entity testEntity = mock(Entity.class);
    when(mockEntityClientService.create(any(EntityClientEndpoint.class))).thenReturn(testEntity);
    EntityClientEndpoint mockTestEntityClientEndpoint = mock(EntityClientEndpoint.class);
    when(mockClientEntityManager.fetchEntity(any(EntityDescriptor.class), any(Runnable.class))).thenReturn(mockTestEntityClientEndpoint);
    
    // Now, run the test.
    long version = 1;
// clientids start at 1
    TerracottaEntityRef<Entity, Void> testRef = new TerracottaEntityRef(mockClientEntityManager, mockMaintenanceModeService, Entity.class, version, "TEST", mockEntityClientService, new AtomicLong(1));
    Entity entity1 = testRef.fetchEntity();
    verify(mockMaintenanceModeService).readLockEntity(Entity.class, "TEST");
    verify(mockClientEntityManager).fetchEntity(eq(new EntityDescriptor(new EntityID(Entity.class.getName(), "TEST"), new ClientInstanceID(1), version)), any(Runnable.class));
    Assert.assertNotNull(entity1);
    Assert.assertEquals(testEntity, entity1);
    entity1.close();
    // Note that we don't see the corresponding readUnlockEntity call since it is called by the EntityClientEndpoint, when closed, but that is just a mock.
  }
}
