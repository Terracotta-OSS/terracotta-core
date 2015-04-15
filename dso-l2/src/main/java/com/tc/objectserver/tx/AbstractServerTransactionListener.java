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

import com.tc.net.NodeID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.Set;

public abstract class AbstractServerTransactionListener implements ServerTransactionListener {

  @Override
  public void addResentServerTransactionIDs(final Collection stxIDs) {
    // Override if you want
  }

  @Override
  public void clearAllTransactionsFor(final NodeID deadNode) {
    // Override if you want
  }

  @Override
  public void incomingTransactions(final NodeID source, final Set serverTxnIDs) {
    // Override if you want
  }

  @Override
  public void transactionApplied(final ServerTransactionID stxID, final ObjectIDSet newObjectsCreated) {
    // Override if you want
  }

  @Override
  public void transactionCompleted(final ServerTransactionID stxID) {
    // Override if you want
  }

  @Override
  public void transactionManagerStarted(final Set cids) {
    // Override if you want
  }
}
