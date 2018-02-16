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
package com.tc.objectserver.api;

import com.tc.util.Assert;
import java.util.EnumSet;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class ServerEntityActionTest {
  EnumSet<ServerEntityAction> lifecycle = EnumSet.of(
        ServerEntityAction.CREATE_ENTITY,
        ServerEntityAction.DESTROY_ENTITY,
        ServerEntityAction.DISCONNECT_CLIENT,
//        ServerEntityAction.FAILOVER_FLUSH,
        ServerEntityAction.FETCH_ENTITY,
        ServerEntityAction.RELEASE_ENTITY,
        ServerEntityAction.RECONFIGURE_ENTITY);
  
  public ServerEntityActionTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
  }
  
  @After
  public void tearDown() {
  }

  @Test
  public void testLifecycle() {    
    lifecycle.forEach(action->Assert.assertTrue(action.isLifecycle()));
    
    EnumSet<ServerEntityAction> non = EnumSet.complementOf(lifecycle);

    non.forEach(action->Assert.assertFalse(action.isLifecycle()));
  }

  @Test
  public void testReplication() {
    EnumSet<ServerEntityAction> replication = EnumSet.copyOf(lifecycle);
    replication.add(ServerEntityAction.INVOKE_ACTION);
    replication.add(ServerEntityAction.REQUEST_SYNC_ENTITY);
    replication.add(ServerEntityAction.ORDER_PLACEHOLDER_ONLY);
    
    replication.forEach(action->Assert.assertTrue(action.isReplicated()));
    
    EnumSet<ServerEntityAction> non = EnumSet.complementOf(replication);

    non.forEach(action->Assert.assertFalse(action.isReplicated()));
  }
    
}
