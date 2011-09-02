/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.NodeID;
import com.tc.object.msg.CommitTransactionMessage;
import com.tc.object.msg.CommitTransactionMessageFactory;

import java.util.LinkedList;
import java.util.List;

public class TestCommitTransactionMessageFactory implements CommitTransactionMessageFactory {

  public final List messages = new LinkedList();

  public CommitTransactionMessage newCommitTransactionMessage(NodeID remoteNode) {
    CommitTransactionMessage rv = new TestCommitTransactionMessage();
    messages.add(rv);
    return rv;
  }

}
