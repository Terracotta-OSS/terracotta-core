/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package deadlock;

import EDU.oswego.cs.dl.util.concurrent.CountDown;

/**
 * This class will always deadlock when run. If you want to run the deadlock detector included with the VM, just fire up this program 
 */
public class DeadLock {

  public static void main(String[] args) throws Exception {
    final Object lock1 = new Object();
    final Object lock2 = new Object();
    final Object lock3 = new Object();

    final CountDown count = new CountDown(3);

    Thread t1 = new Thread("thread 1") {
      public void run() {
        try {
          synchronized (lock1) {
            count.release();
            count.acquire();
            synchronized (lock2) {
              throw new Error("Not supposed to get here");
            }
          }
        } catch (Throwable t) {
          t.printStackTrace();
        }
      }
    };
    t1.start();

    Thread t2 = new Thread("thread 2") {
      public void run() {
        try {
          synchronized (lock2) {
            count.release();
            count.acquire();
            synchronized (lock3) {
              throw new Error("Not supposed to get here");
            }
          }
        } catch (Throwable t) {
          t.printStackTrace();
        }
      }
    };
    t2.start();

    Thread t3 = new Thread("thread 3") {
      public void run() {
        try {
          synchronized (lock3) {
            count.release();
            count.acquire();
            synchronized (lock1) {
              throw new Error("Not supposed to get here");
            }
          }
        } catch (Throwable t) {
          t.printStackTrace();
        }
      }
    };
    t3.start();

    t1.join();
    t2.join();
    t3.join();
  }

}
