/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.msg.MessageRecyclerImpl;
import com.tc.object.tx.ServerTransactionID;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class CommitTransactionMessageRecycler extends MessageRecyclerImpl implements ServerTransactionListener {

  private final Set resentTransactionIDs = new HashSet();

  public CommitTransactionMessageRecycler(ServerTransactionManager transactionManager) {
    super();
    transactionManager.addTransactionListener(this);
  }

  public synchronized void addMessage(DSOMessageBase message, Set keys) {
    if (resentTransactionIDs.size() > 0) {
      if (resentTransactionIDs.removeAll(keys)) {
        // Dont manage this Message
        if (false) System.err.println("MessageRecycler :: ignoring resent message : resentTxIDs.size() = "
                                      + resentTransactionIDs.size());
        return;
      }
    }
    super.addMessage(message, keys);
  }

  public void transactionCompleted(ServerTransactionID stxID) {
    recycle(stxID);
  }

  public void addResentServerTransactionIDs(Collection stxIDs) {
    resentTransactionIDs.addAll(stxIDs);
  }

  public void clearAllTransactionsFor(ChannelID client) {
    for (Iterator iter = resentTransactionIDs.iterator(); iter.hasNext();) {
      ServerTransactionID stxID = (ServerTransactionID) iter.next();
      if (stxID.getChannelID().equals(client)) {
        iter.remove();
      }
    }
  }

  public void transactionApplied(ServerTransactionID stxID) {
    return;
  }

  public void incomingTransactions(ChannelID cid, Set serverTxnIDs) {
    return;
  }

}
