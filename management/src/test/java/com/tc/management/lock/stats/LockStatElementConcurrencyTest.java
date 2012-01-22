/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.lock.stats;


import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.locks.LockID;
import com.tc.object.locks.StringLockID;
import com.tc.test.TCTestCase;

import java.util.ArrayList;
import java.util.Collection;

public class LockStatElementConcurrencyTest extends TCTestCase {
  public void testSerializeTo() throws Exception {
    LockStatElement element = createRootLockStatElement();
    populateWithInitialChildren(element);

    ThreadPopulateOneMore oneMore = new ThreadPopulateOneMore(element);
    oneMore.start();

    element.serializeTo(new TCByteBufferOutputStream());

    oneMore.join();
  }

  public void testToString() throws Exception {
    LockStatElement element = createRootLockStatElement();
    populateWithInitialChildren(element);

    ThreadPopulateOneMore oneMore = new ThreadPopulateOneMore(element);
    oneMore.start();

    element.toString();

    oneMore.join();
  }

  public class ThreadPopulateOneMore extends Thread {
    private LockStatElement element;

    public ThreadPopulateOneMore(LockStatElement element) {
      this.element = element;
    }

    public void run() {
      synchronized (LockStatElementConcurrencyTest.this) {
        try {
          LockStatElementConcurrencyTest.this.wait();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
      populateWithOneMoreChild(element);
    }
  }

  public class LockStatElementSlow extends LockStatElement {
    private static final long serialVersionUID = 1011206427765731291L;

    public LockStatElementSlow(final LockID lockID, final StackTraceElement stackTraceElement) {
      super(lockID, stackTraceElement);
    }

    public String toString() {
      try {
        Thread.sleep(20);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      synchronized (LockStatElementConcurrencyTest.this) {
        LockStatElementConcurrencyTest.this.notifyAll();
      }

      return super.toString();
    }

    public void serializeTo(final TCByteBufferOutput serialOutput) {
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      
      synchronized (LockStatElementConcurrencyTest.this) {
        LockStatElementConcurrencyTest.this.notifyAll();
      }

      super.serializeTo(serialOutput);
    }
  }

  private LockStatElement createRootLockStatElement() {
    return new LockStatElement(new StringLockID("testLock0"), new Exception().getStackTrace()[0]);
  }

  private LockStatElement createSlowLockStatElement(String id) {
    return new LockStatElementSlow(new StringLockID(id), new Exception().getStackTrace()[0]);
  }

  private void populateWithInitialChildren(final LockStatElement element) {
    Collection children1 = new ArrayList();
    for (int i = 1; i <= 10; i++) {
      children1.add(createSlowLockStatElement("testLock"+i));
    }
    element.setChild(children1);
  }

  private void populateWithOneMoreChild(final LockStatElement element) {
    Collection children2 = new ArrayList();
    children2.add(createSlowLockStatElement("testLock11"));
    element.setChild(children2);
  }
}
