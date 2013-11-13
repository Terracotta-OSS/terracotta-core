/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

import com.tc.net.ClientID;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.dna.api.LogicalChangeResult;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.persistence.TransactionPersistorImpl.GlobalTransactionDescriptorSerializer;
import com.tc.test.TCTestCase;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

public class GlobalTransactionDescriptorSerializerTest extends TCTestCase {
  public void testSerialization() throws Exception {
    GlobalTransactionDescriptor desc = new GlobalTransactionDescriptor(new ServerTransactionID(new ClientID(1),
                                                                                               new TransactionID(2)),
                                                                       new GlobalTransactionID(3));
    Map<LogicalChangeID, LogicalChangeResult> results = new HashMap<LogicalChangeID, LogicalChangeResult>();
    results.put(new LogicalChangeID(4), LogicalChangeResult.SUCCESS);
    results.put(new LogicalChangeID(5), LogicalChangeResult.FAILURE);
    desc.recordLogicalChangeResults(results);
    ByteBuffer buffer = GlobalTransactionDescriptorSerializer.INSTANCE.transform(desc);
    GlobalTransactionDescriptor recoveredDesc = GlobalTransactionDescriptorSerializer.INSTANCE.recover(buffer);
    Assert.assertEquals(recoveredDesc, desc);
    Assert.assertEquals(2, recoveredDesc.getApplyResults().size());
    Assert.assertEquals(LogicalChangeResult.SUCCESS, recoveredDesc.getApplyResults().get(new LogicalChangeID(4)));
    Assert.assertEquals(LogicalChangeResult.FAILURE, recoveredDesc.getApplyResults().get(new LogicalChangeID(5)));

  }
}
