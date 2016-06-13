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
package com.tc.objectserver.handler;

import com.tc.net.ClientID;
import com.tc.object.EntityID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.persistence.EntityPersistor;
import com.tc.util.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class EntityExistenceHelpersTest {
  
  public EntityExistenceHelpersTest() {
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

  /**
   * Test of createEntityReturnWasCached method, of class EntityExistenceHelpers.
   */
  @Test
  public void testCreateEntityReturnWasCached() throws Exception {
    EntityPersistor persistor = mock(EntityPersistor.class);
    EntityManager manager = mock(EntityManager.class);
    byte[] configuration = new byte[0];
    ClientID cid = mock(ClientID.class);
    TransactionID tid = mock(TransactionID.class);
    EntityID eid = new EntityID("test", "test");
    when(cid.isNull()).thenReturn(Boolean.FALSE);
    boolean result = EntityExistenceHelpers.createEntityReturnWasCached(persistor, manager, cid, tid, TransactionID.NULL_ID, eid, 1, 1, configuration, true);
// first run through should be false
    Assert.assertFalse(result);
    when(persistor.wasEntityCreatedInJournal(any(), anyLong())).thenReturn(Boolean.TRUE);
    result = EntityExistenceHelpers.createEntityReturnWasCached(persistor, manager, cid, tid, TransactionID.NULL_ID, eid, 1, 1, configuration, true);
// if the persistor thinks the entity was created, the entity return was cached
    Assert.assertTrue(result);
    when(cid.isNull()).thenReturn(Boolean.TRUE);
    result = EntityExistenceHelpers.createEntityReturnWasCached(persistor, manager, cid, tid, TransactionID.NULL_ID, eid, 1, 1, configuration, true);
// if the client id is null then the transaction was generated on the server and the return is never cached and the create never resent
    Assert.assertFalse(result);
  }
}
