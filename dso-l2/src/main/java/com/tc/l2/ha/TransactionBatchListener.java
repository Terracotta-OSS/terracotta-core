/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.object.msg.CommitTransactionMessage;

public interface TransactionBatchListener {

  public void notifyTransactionBatchAdded(CommitTransactionMessage ctm);

}
