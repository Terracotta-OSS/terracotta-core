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
package com.tc.objectserver.tx;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.mockito.InOrder;

import com.tc.async.api.EventContext;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.context.ApplyTransactionContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.event.ClientChannelMonitor;
import com.tc.objectserver.event.ServerEventBuffer;
import com.tc.objectserver.gtx.TestGlobalTransactionManager;
import com.tc.objectserver.impl.TestObjectManager;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.test.TCTestCase;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class TransactionalObjectManagerTest extends TCTestCase {

  private TestObjectManager                      objectManager;
  private TestTransactionalStageCoordinator      coordinator;
  private TransactionalObjectManagerImpl txObjectManager;
  private TestGlobalTransactionManager gtxMgr;
  private ServerEventBuffer                 serverEventBuffer;
  private ClientChannelMonitor              clientChannelMonitor;

  @Override
  public void setUp() {
    this.objectManager = spy(new TestObjectManager());
    this.coordinator = spy(new TestTransactionalStageCoordinator());
    this.gtxMgr = new TestGlobalTransactionManager();
    this.txObjectManager = new TransactionalObjectManagerImpl(this.objectManager, gtxMgr, this.coordinator);
    ServerConfigurationContext scc = mock(ServerConfigurationContext.class);
    when(scc.getTransactionManager()).thenReturn(new TestServerTransactionManager());
    serverEventBuffer = mock(ServerEventBuffer.class);
    clientChannelMonitor = mock(ClientChannelMonitor.class);
  }

  public void testSimpleLookup() throws Exception {
    objectManager.addExistingObjectIDs(asCollectionOfObjectIDs(2L, 3L));
    txObjectManager.addTransactions(asList(createTransaction(1, asList(1L), asList(2L, 3L))));
    verify(coordinator).initiateLookup();

    txObjectManager.lookupObjectsForTransactions();
    verify(coordinator).addToApplyStage(argThat(hasTransactionID(1)));

    ApplyTransactionInfo applyTransactionInfo = applyInfoWithTransactionID(1);
    txObjectManager.applyTransactionComplete(applyTransactionInfo);
    verify(applyTransactionInfo).addObjectsToBeReleased(anyCollection());
  }

  public void testOverlappedLookups() throws Exception {
    objectManager.addExistingObjectIDs(asCollectionOfObjectIDs(1L, 2L, 3L));
    txObjectManager.addTransactions(asList(createTransaction(1, Collections.EMPTY_SET, asList(1L, 2L))));

    txObjectManager.lookupObjectsForTransactions();
    verify(coordinator).addToApplyStage(argThat(hasTransactionID(1)));

    txObjectManager.addTransactions(asList(createTransaction(2, Collections.EMPTY_SET, asList(2L, 3L))));
    verify(coordinator, never()).addToApplyStage(argThat(hasTransactionID(2)));

    ApplyTransactionInfo applyTransactionInfo = applyInfoWithTransactionID(1);
    txObjectManager.applyTransactionComplete(applyTransactionInfo);

    objectManager.releaseAll(applyTransactionInfo.getObjectsToRelease());

    txObjectManager.lookupObjectsForTransactions();
    verify(coordinator).addToApplyStage(argThat(hasTransactionID(2)));
  }

  public void testProcessPendingInOrder() throws Exception {
    objectManager.addExistingObjectIDs(asCollectionOfObjectIDs(1L, 2L));
    txObjectManager.addTransactions(asList(createTransaction(1, Collections.EMPTY_SET, asList(1L))));
    txObjectManager.lookupObjectsForTransactions();
    verify(coordinator).addToApplyStage(argThat(hasTransactionID(1)));

    // Finish the transaction but don't release Object1 yet, this will force later transactions to go pending on object1
    ApplyTransactionInfo applyTransactionInfo1 = applyInfoWithTransactionID(1);
    txObjectManager.applyTransactionComplete(applyTransactionInfo1);

    txObjectManager.addTransactions(asList(createTransaction(2, Collections.EMPTY_SET, asList(1L))));
    txObjectManager.lookupObjectsForTransactions();
    verify(coordinator, never()).addToApplyStage(argThat(hasTransactionID(2)));

    // This transaction goes through because it does not use object1
    txObjectManager.addTransactions(asList(createTransaction(3, Collections.EMPTY_SET, asList(2L))));
    txObjectManager.lookupObjectsForTransactions();
    verify(coordinator).addToApplyStage(argThat(hasTransactionID(3)));

    txObjectManager.addTransactions(asList(createTransaction(4, Collections.EMPTY_SET, asList(1L)),
        createTransaction(5, Collections.EMPTY_SET, asList(1L))));
    txObjectManager.lookupObjectsForTransactions();
    verify(coordinator, never()).addToApplyStage(argThat(hasTransactionID(4)));
    verify(coordinator, never()).addToApplyStage(argThat(hasTransactionID(5)));

    // Release the object and verify that all unblocked transactions are run in order.
    objectManager.releaseAll(applyTransactionInfo1.getObjectsToRelease());

    InOrder inOrder = inOrder(coordinator);
    txObjectManager.lookupObjectsForTransactions();
    inOrder.verify(coordinator).addToApplyStage(argThat(hasTransactionID(2)));
    inOrder.verify(coordinator).addToApplyStage(argThat(hasTransactionID(4)));
    inOrder.verify(coordinator).addToApplyStage(argThat(hasTransactionID(5)));

    ApplyTransactionInfo applyTransactionInfo2 = applyInfoWithTransactionID(3);
    txObjectManager.applyTransactionComplete(applyTransactionInfo2);
    objectManager.releaseAll(applyTransactionInfo2.getObjectsToRelease());

    txObjectManager.applyTransactionComplete(applyInfoWithTransactionID(2));
    txObjectManager.applyTransactionComplete(applyInfoWithTransactionID(4));

    ApplyTransactionInfo applyTransactionInfo5 = applyInfoWithTransactionID(5);
    txObjectManager.applyTransactionComplete(applyTransactionInfo5);
    assertThat(applyTransactionInfo5.getObjectsToRelease(), containsObjectWithID(new ObjectID(1)));
  }

  public void testAlreadyCommittedTransaction() throws Exception {
    objectManager.addExistingObjectIDs(asCollectionOfObjectIDs(1L));
    gtxMgr.commit(new ServerTransactionID(new ClientID(0), new TransactionID(1)));
    txObjectManager.addTransactions(asList(createTransaction(1, Collections.EMPTY_SET, asList(1L))));
    txObjectManager.lookupObjectsForTransactions();
    verify(coordinator).addToApplyStage((EventContext) argThat(allOf(hasTransactionID(1), not(needsApply()))));

    ApplyTransactionInfo applyTransactionInfo = applyInfoWithTransactionID(1);
    txObjectManager.applyTransactionComplete(applyTransactionInfo);
    assertThat(applyTransactionInfo.getObjectsToRelease(), containsObjectWithID(new ObjectID(1L)));
    objectManager.releaseAll(applyTransactionInfo.getObjectsToRelease());
  }

  public void testCheckoutBatching() throws Exception {
    objectManager.addExistingObjectIDs(asCollectionOfObjectIDs(1L, 3L));
    txObjectManager.addTransactions(asList(createTransaction(1, Collections.EMPTY_SET, asList(1L))));
    txObjectManager.addTransactions(asList(createTransaction(2, asList(2L), asList(1L))));
    txObjectManager.addTransactions(asList(createTransaction(3, Collections.EMPTY_SET, asList(1L, 3L))));
    txObjectManager.lookupObjectsForTransactions();

    InOrder inOrder = inOrder(coordinator);
    inOrder.verify(coordinator).addToApplyStage(argThat(hasTransactionID(1)));
    inOrder.verify(coordinator).addToApplyStage(argThat(hasTransactionID(2)));
    inOrder.verify(coordinator, never()).addToApplyStage(argThat(hasTransactionID(3)));

    ApplyTransactionInfo applyTransactionInfo1 = applyInfoWithTransactionID(1);
    txObjectManager.applyTransactionComplete(applyTransactionInfo1);
    verify(applyTransactionInfo1, never()).addObjectsToBeReleased(anyCollection());

    ApplyTransactionInfo applyTransactionInfo2 = applyInfoWithTransactionID(2);
    txObjectManager.applyTransactionComplete(applyTransactionInfo2);
    verify(applyTransactionInfo2).addObjectsToBeReleased((Collection<ManagedObject>) argThat(containsObjectWithID(new ObjectID(1))));
    objectManager.releaseAll(applyTransactionInfo2.getObjectsToRelease());

    txObjectManager.lookupObjectsForTransactions();

    verify(coordinator).addToApplyStage(argThat(hasTransactionID(3)));
  }

  public void testBlockedMergeCheckout() throws Exception {
    objectManager.addExistingObjectIDs(asCollectionOfObjectIDs(1L, 2L));
    txObjectManager.addTransactions(asList(createTransaction(1, Collections.EMPTY_SET, asList(1L))));
    txObjectManager.lookupObjectsForTransactions();
    verify(coordinator).addToApplyStage(argThat(hasTransactionID(1)));

    ManagedObject o = objectManager.getObjectByID(new ObjectID(2L));
    txObjectManager.addTransactions(asList(createTransaction(2, asList(2L), asList(1L, 2L))));
    txObjectManager.lookupObjectsForTransactions();

    verify(coordinator, never()).addToApplyStage(argThat(hasTransactionID(2)));

    ApplyTransactionInfo applyTransactionInfo = applyInfoWithTransactionID(1);
    txObjectManager.applyTransactionComplete(applyTransactionInfo);
    assertThat(applyTransactionInfo.getObjectsToRelease(), containsObjectWithID(new ObjectID(1L)));
    objectManager.releaseAll(applyTransactionInfo.getObjectsToRelease());

    // Another transaction on the newly released object can't go through because another earlier transaction
    // is waiting on that object.
    txObjectManager.addTransactions(asList(createTransaction(3, Collections.EMPTY_SET, asList(1L))));
    txObjectManager.lookupObjectsForTransactions();
    verify(coordinator, never()).addToApplyStage(argThat(hasTransactionID(3)));

    objectManager.release(o);
    txObjectManager.lookupObjectsForTransactions();
    InOrder inOrder = inOrder(coordinator);
    inOrder.verify(coordinator).addToApplyStage(argThat(hasTransactionID(2)));
    inOrder.verify(coordinator).addToApplyStage(argThat(hasTransactionID(3)));
  }

  public void testSkipApplyWithMissingNewObject() throws Exception {
    objectManager.addExistingObjectIDs(asCollectionOfObjectIDs(2L));
    ServerTransaction tx = createTransaction(0, asList(1L), asList(2L));
    gtxMgr.commit(tx.getServerTransactionID());

    txObjectManager.addTransactions(asList(tx));
    txObjectManager.lookupObjectsForTransactions();

    verify(objectManager, never()).createNewObjects((Set) argThat(containsObjectWithID(new ObjectID(1L))));
    verify(coordinator).addToApplyStage((EventContext) argThat(allOf(hasTransactionID(0), hasIgnorableObject(new ObjectID(1L)))));
  }

  private static Collection<ObjectID> asCollectionOfObjectIDs(Long ... longs) {
    Set<ObjectID> oids = new BitSetObjectIDSet();
    for (long l : longs) {
      oids.add(new ObjectID(l));
    }
    return oids;
  }

  private ApplyTransactionInfo applyInfoWithTransactionID(long transactionID) {
    return spy(new ApplyTransactionInfo(true,
                                        new ServerTransactionID(new ClientID(0), new TransactionID(transactionID)),
                                        GlobalTransactionID.NULL_ID, true, false, serverEventBuffer,
                                        clientChannelMonitor));
  }

  private <T> Matcher<T> containsObjectWithID(final ObjectID id) {
    return new BaseMatcher<T>() {
      @Override
      public boolean matches(final Object o) {
        if (o instanceof Collection) {
          for (Object obj : (Collection) o) {
            if (obj instanceof ManagedObject && id.equals(((ManagedObject)obj).getID())) {
              return true;
            }
          }
        }
        return false;
      }

      @Override
      public void describeTo(final Description description) {
        //
      }
    };
  }

  private Matcher<ApplyTransactionContext> hasIgnorableObject(final ObjectID oid) {
    return new BaseMatcher<ApplyTransactionContext>() {
      @Override
      public boolean matches(final Object o) {
        if (o instanceof ApplyTransactionContext) {
          return ((ApplyTransactionContext)o).getIgnoredObjects().contains(oid);
        } else {
          return false;
        }
      }

      @Override
      public void describeTo(final Description description) {

      }
    };
  }

  private Matcher<ApplyTransactionContext> hasTransactionID(final long transactionID) {
    return new BaseMatcher<ApplyTransactionContext>() {
      @Override
      public boolean matches(final Object o) {
        if (o instanceof ApplyTransactionContext) {
          return ((ApplyTransactionContext)o).getTxn()
              .getServerTransactionID()
              .equals(new ServerTransactionID(new ClientID(0), new TransactionID(transactionID)));
        } else {
          return false;
        }
      }

      @Override
      public void describeTo(final Description description) {
        //
      }
    };
  }

  private Matcher<ApplyTransactionContext> needsApply() {
    return new BaseMatcher<ApplyTransactionContext>() {
      @Override
      public boolean matches(final Object o) {
        if (o instanceof ApplyTransactionContext) {
          return ((ApplyTransactionContext)o).needsApply();
        } else {
          return false;
        }
      }

      @Override
      public void describeTo(final Description description) {
        //
      }
    };
  }

  private ServerTransaction createTransaction(long txId, Collection<Long> newObjects, Collection<Long> objects) {
    ServerTransaction transaction = mock(ServerTransaction.class);
    ObjectIDSet newObjectIDs = new BitSetObjectIDSet();
    for (long l : newObjects) {
      newObjectIDs.add(new ObjectID(l));
    }
    ObjectIDSet objectIDs = new BitSetObjectIDSet(newObjectIDs);
    for (long l : objects) {
      objectIDs.add(new ObjectID(l));
    }
    when(transaction.getServerTransactionID()).thenReturn(new ServerTransactionID(new ClientID(0), new TransactionID(txId)));
    when(transaction.getNewObjectIDs()).thenReturn(newObjectIDs);
    when(transaction.getObjectIDs()).thenReturn(objectIDs);
    return transaction;
  }
}
