/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.api;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.groups.ClientID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.TimerSpec;
import com.tc.util.Assert;

import junit.framework.TestCase;

public class LockContextTest extends TestCase {

  public void testHashCode() throws Exception {

    LockContext lc = new LockContext(new LockID("lock tock"), new ClientID(new ChannelID(2)), new ThreadID(3),
                                     LockLevel.WRITE, "lock-type");

    WaitContext wc = new WaitContext(new LockID("lock tock"), new ClientID(new ChannelID(2)), new ThreadID(3),
                                     LockLevel.WRITE, "lock-type", new TimerSpec());

    TryLockContext tlc = new TryLockContext(new LockID("lock tock"), new ClientID(new ChannelID(2)), new ThreadID(3),
                                            LockLevel.WRITE, "lock-type", new TimerSpec());

    assertHashCodeAfterDeserialize(lc);
    assertHashCodeAfterDeserialize(wc);
    assertHashCodeAfterDeserialize(tlc);
  }

  private static void assertHashCodeAfterDeserialize(LockContext o) throws Exception {
    int hash = o.hashCode();

    // This isn't a great thing to do (since in theory the hash value could be 0 if the algorithm is changed), but
    // without this assertion the test on the deserialized hashcode doesn't mean much
    Assert.assertTrue("hash=" + hash, hash != 0);

    TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    o.serializeTo(out);

    LockContext lc2 = (LockContext) o.getClass().newInstance();
    lc2.deserializeFrom(new TCByteBufferInputStream(out.toArray()));

    Assert.assertEquals(hash, lc2.hashCode());
  }

}
