/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import java.util.Arrays;
import java.util.Random;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionBatch;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author steve
 */
public class CommitTransactionMessageTest {

  @Test
  public void testMessage() throws Exception {
    Random rnd = new Random();

    for (int i = 0; i < 100; i++) {
      int len = rnd.nextInt(40960);

      byte orig[] = new byte[len];
      rnd.nextBytes(orig);

      TCByteBufferOutputStream bbos = new TCByteBufferOutputStream();
      bbos.write(orig);

      TransactionBatch batch = mock(TransactionBatch.class);
      when(batch.getData()).thenReturn(bbos.toArray());

      CommitTransactionMessageImpl msg = new CommitTransactionMessageImpl(new SessionID(0), mock(MessageMonitor.class),
                                                                          new TCByteBufferOutputStream(4, 4096, false),
                                                                          null,
                                                                          TCMessageType.COMMIT_TRANSACTION_MESSAGE);
      ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
      msg.setBatch(batch, serializer);
      msg.dehydrate();

      CommitTransactionMessageImpl msg2 = new CommitTransactionMessageImpl(SessionID.NULL_ID, mock(MessageMonitor.class),
                                                                           null, (TCMessageHeader) msg.getHeader(),
                                                                           msg.getPayload());
      msg2.hydrate();

      TCByteBufferInputStream bbis = new TCByteBufferInputStream(msg2.getBatchData());
      byte[] compare = new byte[orig.length];
      int read = bbis.read(compare);
      assertEquals(compare.length, read);
      assertTrue(Arrays.equals(orig, compare));
    }

  }

}
