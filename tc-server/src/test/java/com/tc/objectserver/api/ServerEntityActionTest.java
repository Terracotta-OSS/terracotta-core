/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.objectserver.api;

import com.tc.util.Assert;
import java.util.EnumSet;
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
