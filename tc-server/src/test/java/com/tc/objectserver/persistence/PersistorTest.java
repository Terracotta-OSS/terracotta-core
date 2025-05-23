/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.tc.objectserver.persistence;

import com.tc.net.ClientID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;
import com.tc.net.core.ProductID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
    IPlatformPersistence persist = mock(IPlatformPersistence.class);
    ConcurrentHashMap<ClientID, Boolean> clientsMap = new ConcurrentHashMap<>();
    clientsMap.put(cid, true);
    clientsMap.put(new ClientID(2), false);
    clientsMap.put(new ClientID(3), true);
    clientsMap.put(new ClientID(4), false);

    when(persist.loadDataElement(eq("clients_map.map"))).thenReturn(clientsMap);
        
    Persistor persistor = new Persistor(persist);
    persistor.start(true);

    Set<ClientID> orphans = persistor.getClientStatePersistor().loadOrphanClientIDs();
    Set<ClientID> clients = persistor.getClientStatePersistor().loadPermanentClientIDs();
    Set<ClientID> all = persistor.getClientStatePersistor().loadAllClientIDs();

    Assert.assertTrue(clients.contains(new ClientID(1)));
    Assert.assertTrue(clients.contains(new ClientID(3)));
    Assert.assertEquals(0, orphans.size());
    Assert.assertEquals(2, clients.size());
    Assert.assertEquals(2, all.size());
    persistor.addClientState(new ClientID(5), ProductID.STRIPE);
    all = persistor.getClientStatePersistor().loadAllClientIDs();
    Assert.assertEquals(3, all.size());
  }
}
