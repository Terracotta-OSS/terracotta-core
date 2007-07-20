/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionBatch;
import com.tc.object.tx.TransactionID;
import com.tc.test.TCTestCase;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * @author steve
 */
public class CommitTransactionMessageTest extends TCTestCase {

  public void testMessage() throws Exception {
    Random rnd = new Random();

    for (int i = 0; i < 100; i++) {
      int len = rnd.nextInt(40960);

      byte orig[] = new byte[len];
      rnd.nextBytes(orig);

      TCByteBufferOutputStream bbos = new TCByteBufferOutputStream();
      bbos.write(orig);

      Set acknowledged = new HashSet();
      for (int j = 0; j < 10; j++) {
        acknowledged.add(new TransactionID(j));
      }

      TransactionBatch batch = new TestTransactionBatch(bbos.toArray(), acknowledged);

      CommitTransactionMessageImpl msg = new CommitTransactionMessageImpl(new SessionID(0), new NullMessageMonitor(),
                                                                          new TCByteBufferOutputStream(4, 4096, false),
                                                                          null,
                                                                          TCMessageType.COMMIT_TRANSACTION_MESSAGE);
      ObjectStringSerializer serializer = new ObjectStringSerializer();
      msg.setBatch(batch, serializer);
      msg.dehydrate();

      CommitTransactionMessageImpl msg2 = new CommitTransactionMessageImpl(SessionID.NULL_ID, new NullMessageMonitor(),
                                                                           null, (TCMessageHeader) msg.getHeader(), msg
                                                                               .getPayload());
      msg2.hydrate();

      assertEquals(acknowledged, new HashSet(msg2.getAcknowledgedTransactionIDs()));

      TCByteBufferInputStream bbis = new TCByteBufferInputStream(msg2.getBatchData());
      byte[] compare = new byte[orig.length];
      int read = bbis.read(compare);
      assertEquals(compare.length, read);
      assertTrue(Arrays.equals(orig, compare));
    }

  }

}
