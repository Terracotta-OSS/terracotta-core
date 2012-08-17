/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.dump.DumpServer;

import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

public class L2DumperTestClient extends ClientBase {

  List<Integer> mySharedArrayList;

  public L2DumperTestClient(String[] args) {
    super(args);
  }

  @Override
  public void test(Toolkit toolkit) {
    mySharedArrayList = toolkit.getList("sharedList", null);
    verify(0);
    int index = waitOnBarrier();

    int totalAdditions = 3000;

    if (index == 0) {
      addToList(totalAdditions);
    }

    waitOnBarrier();

    verify(totalAdditions);

    if (index == 0) {
      takeServerDump();
    }

    waitOnBarrier();
  }

  private void takeServerDump() {
    try {
      long time = System.currentTimeMillis();
      new DumpServer("localhost", getGroupData(0).getJmxPort(0)).dumpServer();
      System.out.println("Time taken for dump = " + (System.currentTimeMillis() - time));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void addToList(int totalAdditions) {
    for (int i = 0; i < 3000; i++) {
      synchronized (mySharedArrayList) {
        mySharedArrayList.add(i);
      }
    }
  }

  private int waitOnBarrier() {
    int index = -1;
    try {
      index = getBarrierForAllClients().await();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }

    return index;
  }

  private void verify(int no) {
    synchronized (mySharedArrayList) {
      Iterator<Integer> iter = mySharedArrayList.iterator();
      int temp = -1;
      int counter = 0;

      Assert.assertEquals(mySharedArrayList.size(), no);

      while (iter.hasNext()) {
        temp = iter.next();
        Assert.assertEquals(counter, temp);
        counter++;
      }
    }
  }
}
