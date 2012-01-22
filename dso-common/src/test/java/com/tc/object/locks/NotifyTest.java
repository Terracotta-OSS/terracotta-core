/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.object.locks.LockID;
import com.tc.object.locks.NotifyImpl;
import com.tc.object.locks.StringLockID;
import com.tc.object.locks.ThreadID;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import junit.framework.TestCase;

public class NotifyTest extends TestCase {

  public void testEqualsAndHashCode() throws Exception {
    Set set = new HashSet();
    Collection pairs = new LinkedList();
    Random r = new Random();
    
    for (int i=0; i<1000; i++) {
      long startValue = r.nextLong();
      LockID lockID1 = new StringLockID("" + startValue);
      LockID lockID2 = new StringLockID("" + (startValue +1));
      ThreadID tid1 = new ThreadID(startValue);
      ThreadID tid2 = new ThreadID(startValue + 1);
      
      Notify aa = new NotifyImpl(lockID1, tid1, true);
      Notify ab = new NotifyImpl(lockID1, tid1, true);
      pairs.add(new Notify[] {aa, ab});
      
      Notify ba = new NotifyImpl(lockID1, tid2, true);
      Notify bb = new NotifyImpl(lockID1, tid2, true);
      pairs.add(new Notify[] {ba, bb});
      
      Notify ca = new NotifyImpl(lockID1, tid1, false);
      Notify cb = new NotifyImpl(lockID1, tid1, false);
      pairs.add(new Notify[] {ca, cb});
      
      Notify da = new NotifyImpl(lockID2, tid1, true);
      Notify db = new NotifyImpl(lockID2, tid1, true);
      pairs.add(new Notify[] {da, db});
      
      Notify ea = new NotifyImpl(lockID2, tid2, true);
      Notify eb = new NotifyImpl(lockID2, tid2, true);
      pairs.add(new Notify[] {ea, eb});
      
      Notify fa = new NotifyImpl(lockID2, tid2, false);
      Notify fb = new NotifyImpl(lockID2, tid2, false);
      pairs.add(new Notify[] {fa, fb});
    }
    
    for (Iterator i=pairs.iterator(); i.hasNext();) {
      Notify[] pair = (Notify[]) i.next();
      assertEquals(pair[0], pair[1]);
      assertEquals(pair[0].hashCode(), pair[1].hashCode());
      set.add(pair[0]);
      set.add(pair[1]);
    }
    
    assertEquals(pairs.size(), set.size());
  }
  
}
