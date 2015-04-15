/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.impl;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;

import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.net.ServerID;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.EvictableEntry;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.persistence.EvictionTransactionPersistor;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerMapEvictionEngineTest {
  private ServerMapEvictionEngine serverMapEvictionEngine;
  private Sink lwmUpdateSink;

  @Before
  public void setUp() throws Exception {
    serverMapEvictionEngine = new ServerMapEvictionEngine(mock(ObjectManager.class),
        new ServerTransactionFactory(ServerID.NULL_ID), mock(EvictionTransactionPersistor.class), true);
    ServerConfigurationContext scc = mock(ServerConfigurationContext.class, Answers.RETURNS_MOCKS.get());
    lwmUpdateSink = mock(Sink.class);
    Stage lwmUpdateStage = mock(Stage.class);
    when(lwmUpdateStage.getSink()).thenReturn(lwmUpdateSink);
    when(scc.getStage(ServerConfigurationContext.TRANSACTION_LOWWATERMARK_STAGE)).thenReturn(lwmUpdateStage);
    serverMapEvictionEngine.initializeContext(scc);
  }

  @Test
  public void testEvictionTransactionCompletion() throws Exception {
    ObjectID oid = new ObjectID(1);
    serverMapEvictionEngine.markEvictionInProgress(oid);
    serverMapEvictionEngine.evictFrom(oid,
        Collections.singletonMap((Object) new UTF8ByteDataHolder("test"), mock(EvictableEntry.class)), "foo");
    ServerTransactionID stxID = new ServerTransactionID(ServerID.NULL_ID, new TransactionID(1));
    serverMapEvictionEngine.transactionCompleted(stxID);
  }
}