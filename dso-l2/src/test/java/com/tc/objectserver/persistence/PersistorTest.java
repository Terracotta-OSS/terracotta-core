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
package com.tc.objectserver.persistence;

import com.tc.net.ClientID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;
import com.tc.util.ProductID;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.terracotta.persistence.IPlatformPersistence;

/**
 *
 */
public class PersistorTest {
  
  public PersistorTest() {
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
  public void testStart() throws Throwable {
    IPlatformPersistence persist = mock(IPlatformPersistence.class);
    Persistor persistor = new Persistor(persist);
    persistor.start(true);
    verify(persist, never()).storeDataElement(eq("journal_container.map"), eq(null));
    persistor.start(false);
    verify(persist).storeDataElement(eq("journal_container.map"), eq(null));
  }
  

  @Test
  public void tesAddClientState() throws Throwable {
    try {
      ClientID cid = new ClientID(1);
      TransactionID tid = new TransactionID(1);
      IPlatformPersistence persist = mock(IPlatformPersistence.class);
      Persistor persistor = new Persistor(persist);
      persistor.start(true);
      persistor.addClientState(cid, ProductID.PERMANENT);
      persistor.getTransactionOrderPersistor().updateWithNewMessage(cid, tid, tid);
      verify(persist).fastStoreSequence(eq(1L), any(), eq(1L));
      cid = new ClientID(2);
      persistor.addClientState(cid, ProductID.STRIPE);
      persistor.getTransactionOrderPersistor().updateWithNewMessage(cid, tid, tid);
      verify(persist, never()).fastStoreSequence(eq(2L), any(), eq(1L));
      cid = new ClientID(3);
      persistor.addClientState(cid, ProductID.SERVER);
      persistor.getTransactionOrderPersistor().updateWithNewMessage(cid, tid, tid);
      verify(persist, never()).fastStoreSequence(eq(3L), any(), eq(1L));
    } catch (Throwable t) {
      t.printStackTrace();
      throw t;
    }
  }  
  
  @Test
  public void testRemoveOrphanedClients() throws Throwable {
    ClientID cid = new ClientID(1);
    IPlatformPersistence persist = new NullPlatformPersistentStorage();
    Persistor persistor = new Persistor(persist);
    persistor.start(true);
    persistor.addClientState(cid, ProductID.PERMANENT);
    persistor.addClientState(new ClientID(2), ProductID.STRIPE);
    persistor.addClientState(new ClientID(3), ProductID.PERMANENT);
    persistor.addClientState(new ClientID(4), ProductID.STRIPE);
    Set<ClientID> orphans = persistor.getClientStatePersistor().loadOrphanClientIDs();
    Set<ClientID> clients = persistor.getClientStatePersistor().loadPermanentClientIDs();
    Set<ClientID> all = persistor.getClientStatePersistor().loadAllClientIDs();
    Assert.assertTrue(orphans.contains(new ClientID(2)));
    Assert.assertTrue(orphans.contains(new ClientID(4)));
    Assert.assertTrue(clients.contains(new ClientID(1)));
    Assert.assertTrue(clients.contains(new ClientID(3)));
    Assert.assertEquals(2, orphans.size());
    Assert.assertEquals(2, clients.size());
    Assert.assertEquals(4, all.size());
  }
}
