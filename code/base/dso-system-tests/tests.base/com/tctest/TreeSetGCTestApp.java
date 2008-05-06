/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

public class TreeSetGCTestApp extends AbstractErrorCatchingTransparentApp {
  private static boolean        DEBUG       = true;

  private static final long     DURATION    = 1 * 60 * 1000;                           // 1 minutes
  private static final long     END         = System.currentTimeMillis() + DURATION;
  private static final String   KEY_PREFIX  = "key";
  private static final int      TOTAL_COUNT = 5;

  // roots
  private final CyclicBarrier   barrier     = new CyclicBarrier(getParticipantCount());
  private final Map             root        = new HashMap();
  private final NextToWork      nextToWork;

  private final Random          random;
  private final String          appId;
  private static AssertionError error;

  public TreeSetGCTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.appId = appId;
    nextToWork = new NextToWork(cfg.getGlobalParticipantCount());
    nextToWork.addParticipant(this.appId);

    long seed = new Random().nextLong();
    System.err.println("seed for " + getApplicationId() + " is " + seed);
    random = new Random(seed);
  }

  protected void runTest() throws Throwable {
    final int index = barrier.barrier();

    if (index == 0) {
      populate();
      nextToWork.setFirstToWork(appId);
    }

    barrier.barrier();

    if (index == 0) {
      mutate();
    } else {
      read();
    }

    if (error != null) {
    //  List list = ServerTransactionManagerImpl.applyList;
      //for (Iterator iter = list.iterator(); iter.hasNext();) {
        //String info = (String) iter.next();
        //debugPrintln(info);
      //}

      //list = ClientTransactionManagerImpl.txnList;
      //for (Iterator iter = list.iterator(); iter.hasNext();) {
       // String info = (String) iter.next();
        //debugPrintln(info);
      //}

      //list = SetManagedObjectState.addedList;
      //for (Iterator iter = list.iterator(); iter.hasNext();) {
       // String info = (String) iter.next();
        //debugPrintln(info);
      //}

      //list = SetManagedObjectState.removedList;
      //for (Iterator iter = list.iterator(); iter.hasNext();) {
        //String info = (String) iter.next();
        //debugPrintln(info);
      //}

      //list = ReceiveTransactionHandler.receivedList;
      //for (Iterator iter = list.iterator(); iter.hasNext();) {
        //String info = (String) iter.next();
        //debugPrintln(info);
      //}

      //list = HashSetApplicator.applyList;
      //for (Iterator iter = list.iterator(); iter.hasNext();) {
        //String applyInfo = (String) iter.next();
        //debugPrintln(applyInfo);
      //}

      throw error;
    }
  }

  private void populate() {
    debugPrintln(wrapMessage("populate..."));
    synchronized (root) {
      for (int i = 0, n = TOTAL_COUNT; i < n; i++) {
        root.put(KEY_PREFIX + i, newTreeSet(getRandomNum(), getRandomNum(), i));
      }
      debugPrintln(wrapMessage("root size=[" + root.size() + "]"));
    }
  }

  public void mutate() throws InterruptedException {
    while (!shouldEnd()) {
      if (nextToWork.isNextToWork(appId)) {
        synchronized (root) {
          debugPrintln(wrapMessage("mutate... "));
          int numToMutate = getRandomNum();
          int numToDeleteCreate = getRandomNum();

          for (int i = 0; i < numToDeleteCreate; i++) {
            root.remove(KEY_PREFIX + i);
            debugPrintln(wrapMessage("deleting top-level TreeSet=[" + i + "]"));
          }
          debugPrintln(wrapMessage("deleted [" + numToDeleteCreate + "] TreeSets:  root size=[" + root.size() + "]"));

          for (int i = 0; i < numToDeleteCreate; i++) {
            root.put(KEY_PREFIX + i, newTreeSet(getRandomNum(), getRandomNum(), i));
          }
          debugPrintln(wrapMessage("added [" + numToDeleteCreate + "] TreeSets:   root size=[" + root.size() + "]"));

          debugPrintln(wrapMessage("mutating [" + numToMutate + "] TreeSets"));
          for (int i = 0; i < numToMutate; i++) {
            TreeSet topLevel = (TreeSet) root.get(KEY_PREFIX + i);
            for (Iterator iter = topLevel.iterator(); iter.hasNext();) {
              TreeSetWrapper tsWrapper = (TreeSetWrapper) iter.next();
              TreeSet ts = tsWrapper.getTreeSet();
              int size = ts.size();

              debugPrintln(wrapMessage("ts=[" + i + "][" + tsWrapper.getId() + "] to mutate has size=[" + size + "]"));

              if (size == 0) {
                error = new AssertionError("***** TreeSet=[" + i + "][" + tsWrapper.getId() + "] has size=0 !!");
                return;
              }

              int startPosition = random.nextInt(size);
              if (startPosition == 0) {
                startPosition++;
              }
              for (int j = startPosition; j < size; j++) {
                boolean removedObject = ts.remove(new FooObject(j));
                if (removedObject) {
                  debugPrintln(wrapMessage("removing element=[" + j + "] from TreeSet=[" + i + "][" + tsWrapper.getId()
                                           + "]"));
                } else {
                  debugPrintln(wrapMessage("element=[" + j + "] from TreeSet=[" + i + "][" + tsWrapper.getId()
                                           + "] WAS NOT REMOVED!"));
                }
              }

              size = ts.size();
              String s = "";
              for (Iterator iterator = ts.iterator(); iterator.hasNext();) {
                FooObject element = (FooObject) iterator.next();
                s += "[" + element.getId() + "]";
              }
              debugPrintln(wrapMessage("ts=[" + i + "][" + tsWrapper.getId() + "] after mutate has size=[" + size
                                       + "] contents=[" + s + "]"));
            }
          }
        }
        nextToWork.setNextToWork();
      } else {
        Thread.sleep(1000);
      }
    }
  }

  private void read() throws InterruptedException {
    while (!shouldEnd()) {
      if (nextToWork.isNextToWork(appId)) {
        synchronized (root) {
          debugPrintln(wrapMessage("reading..."));
          int numToRead = getRandomNum();

          for (int i = 0; i < numToRead; i++) {
            TreeSet topLevel = (TreeSet) root.get(KEY_PREFIX + i);
            for (Iterator iter = topLevel.iterator(); iter.hasNext();) {
              TreeSetWrapper tsWrapper = (TreeSetWrapper) iter.next();
              TreeSet ts = tsWrapper.getTreeSet();
              int size = ts.size();
              for (Iterator iterator = ts.iterator(); iterator.hasNext();) {
                FooObject fo = (FooObject) iterator.next();
                debugPrintln(wrapMessage("#####  FO=[" + fo.getId() + "]"));
              }
              for (int j = 0; j < size; j++) {
                if (!ts.contains(new FooObject(j))) {
                  error = new AssertionError("Element=[" + j + "] missing from TreeSet=[" + i + "]["
                                             + tsWrapper.getId() + "] with size=[" + size + "]  TreeSetContent=["
                                             + getContent(ts) + "]");
                  //debugPrintln("Error occurred... last txn committed in ClientTransactionManagerImpl:  ["
                    //           + ClientTransactionManagerImpl.txnList
                      //             .get(ClientTransactionManagerImpl.txnList.size() - 1) + "]");
                  return;
                }
                debugPrintln(wrapMessage("reading Element=[" + j + "] TreeSet=[" + i + "][" + tsWrapper.getId()
                                         + "] with size=[" + size + "]"));
              }
            }
          }
        }
        nextToWork.setNextToWork();
      } else {
        Thread.sleep(1000);
      }
    }
  }

  private String getContent(TreeSet ts) {
    StringBuffer buffer = new StringBuffer();
    boolean first = true;
    for (Iterator iter = ts.iterator(); iter.hasNext();) {
      FooObject fo = (FooObject) iter.next();
      if (first) {
        first = false;
      } else {
        buffer.append(",");
      }
      buffer.append(fo.getId());
    }
    return buffer.toString();
  }

  private String wrapMessage(String s) {
    return "\n  ##### appId[" + appId + "] " + s + "\n";
  }

  private static void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println("\n " + s);
    }
  }

  private static boolean shouldEnd() {
    return (System.currentTimeMillis() > END) || (error != null);
  }

  private int getRandomNum() {
    int num = random.nextInt(TOTAL_COUNT);
    if (num == 0) {
      num = 1;
    }
    return num;
  }

  private TreeSet newTreeSet(int treeSetCount, int treeSetSize, int id) {
    TreeSet newTS = new TreeSet(new NullTolerantComparator());
    for (int i = 0, n = treeSetCount; i < n; i++) {
      newTS.add(newTreeSetWrapper(treeSetCount, treeSetSize, id, i));
    }
    return newTS;
  }

  private TreeSetWrapper newTreeSetWrapper(int treeSetCount, int treeSetSize, int outer_id, int id) {
    debugPrintln("creating new TreeSet=[" + outer_id + "][" + id + "]:  treeSetCount=[" + treeSetCount
                 + "] treeSetSize=[" + treeSetSize + "]");
    TreeSetWrapper newTS = new TreeSetWrapper(new TreeSet(new NullTolerantComparator()), id);
    for (int i = 0, n = treeSetSize; i < n; i++) {
      newTS.getTreeSet().add(new FooObject(i));
    }
    return newTS;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    new CyclicBarrierSpec().visit(visitor, config);
    config.getOrCreateSpec(FooObject.class.getName());
    config.getOrCreateSpec(NextToWork.class.getName());
    config.getOrCreateSpec(TreeSetWrapper.class.getName());
    config.getOrCreateSpec(NullTolerantComparator.class.getName());

    String testClassName = TreeSetGCTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);
    String methodExpression = "* " + testClassName + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("root", "root");
    spec.addRoot("barrier", "barrier");
    spec.addRoot("nextToWork", "nextToWork");
  }

  private static final class TreeSetWrapper implements Comparable {
    private final int     id;
    private final TreeSet ts;

    public TreeSetWrapper(TreeSet ts, int id) {
      this.id = id;
      this.ts = ts;
    }

    public TreeSet getTreeSet() {
      return ts;
    }

    public int getId() {
      return id;
    }

    public int compareTo(Object o) {
      int othersId = ((TreeSetWrapper) o).getId();
      if (id < othersId) {
        return -1;
      } else if (id == othersId) {
        return 0;
      } else {
        return 1;
      }
    }
  }

  private static final class FooObject implements Comparable {
    private final int id;

    public FooObject(int id) {
      this.id = id;
    }

    public int getId() {
      return id;
    }

    public boolean equals(Object foo) {
      if (foo == null) { return false; }
      return ((FooObject) foo).getId() == id;
    }

    public int hashCode() {
      return id;
    }

    public int compareTo(Object o) {
      int othersId = ((FooObject) o).getId();
      if (id < othersId) {
        return -1;
      } else if (id == othersId) {
        return 0;
      } else {
        return 1;
      }
    }
  }

  private static final class NextToWork {
    private final String[] participants;
    private final int      participantCount;
    private int            nextToWork;
    private int            index;

    public NextToWork(int participantCount) {
      this.participantCount = participantCount;
      participants = new String[this.participantCount];
      nextToWork = 0;
      index = 0;
    }

    public synchronized void addParticipant(String participant) {
      debugPrintln("*****  about to add participant=[" + participant + "]:  participantsLength=[" + index
                   + "] participantCount=[" + participantCount + "]");
      if (index == participantCount) { throw new AssertionError("Participant list is full!  participantCount=["
                                                                + participantCount + "]"); }
      participants[index] = participant;
      index++;
      debugPrintln("*****  added participant=[" + participant + "]");
    }

    public synchronized void setNextToWork() {
      debugPrintln("*****  setting next to work:  before=[" + participants[nextToWork] + "] after=["
                   + participants[((nextToWork + 1) % participantCount)] + "]");
      nextToWork = (nextToWork + 1) % participantCount;
    }

    public synchronized boolean isNextToWork(String participantToCheck) {
      debugPrintln("***** participant=[" + participantToCheck + "] is nextToWork=["
                   + (participants[nextToWork] == participantToCheck) + "]");
      return (participants[nextToWork] == participantToCheck);
    }

    public synchronized void setFirstToWork(String participant) {
      debugPrintln("***** setting first participant to [" + participant + "]");
      for (int i = 0; i < participantCount; i++) {
        if (participants[i] == participant) {
          nextToWork = i;
        }
      }
    }
  }
}
