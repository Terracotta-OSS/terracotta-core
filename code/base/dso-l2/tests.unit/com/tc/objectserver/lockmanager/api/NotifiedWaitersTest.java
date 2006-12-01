/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockRequest;
import com.tc.object.lockmanager.api.ThreadID;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

public class NotifiedWaitersTest extends TestCase {

  public void testBasics() throws Exception {
    ChannelID channel1 = new ChannelID(1);
    ChannelID channel2 = new ChannelID(2);

    Set forChannel1 = new HashSet();
    Set forChannel2 = new HashSet();

    LockID lockID = new LockID("me");
    ThreadID txID1 = new ThreadID(1);
    ThreadID txID2 = new ThreadID(2);
    ThreadID txID3 = new ThreadID(3);

    NotifiedWaiters ns = new NotifiedWaiters();

    LockRequest lr1 = new LockRequest(lockID, txID1, 0);
    forChannel1.add(lr1);
    ns.put(channel1, lr1);

    LockRequest lr2 = new LockRequest(lockID, txID2, 0);
    forChannel1.add(lr2);
    ns.put(channel1, lr2);

    LockRequest lr3 = new LockRequest(lockID, txID3, 0);
    forChannel2.add(lr3);
    ns.put(channel2, lr3);

    assertEquals(forChannel1, ns.getNotifiedFor(channel1));
    assertEquals(forChannel2, ns.getNotifiedFor(channel2));

    ns = new NotifiedWaiters();
    assertTrue(ns.isEmpty());
    ns.getNotifiedFor(new ChannelID(1));
    assertTrue(ns.isEmpty());
  }

}
