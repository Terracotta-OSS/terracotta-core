/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionIDGenerator;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.util.SequenceID;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PassiveServerTransactionImpl extends ServerTransactionImpl implements ServerTransaction {

  public PassiveServerTransactionImpl(GlobalTransactionIDGenerator gtxm, TxnBatchID batchID, TransactionID txID,
                                      SequenceID sequenceID, LockID[] lockIDs, ChannelID channelID, List dnas,
                                      ObjectStringSerializer serializer, Map newRoots, TxnType transactionType,
                                      Collection notifies, DmiDescriptor[] dmis) {
    super(gtxm, batchID, txID, sequenceID, lockIDs, channelID, dnas, serializer, newRoots, transactionType, notifies,
          dmis);
  }

  public SequenceID getClientSequenceID() {
    throw new UnsupportedOperationException();
  }

  public DmiDescriptor[] getDmiDescriptors() {
    throw new UnsupportedOperationException();
  }

  public Collection getNotifies() {
    return Collections.EMPTY_LIST;
  }

  public ObjectStringSerializer getSerializer() {
    throw new UnsupportedOperationException();
  }

  public boolean needsBroadcast() {
    return false;
  }

  public String toString() {
    return "PassiveServerTransactionImpl [ " + super.toString() + " ]";
  }
}
