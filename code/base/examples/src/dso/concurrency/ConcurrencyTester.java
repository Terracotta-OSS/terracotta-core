/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package dso.concurrency;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alan Brown Date: May 17, 2005 Time: 1:55:05 PM
 */
public class ConcurrencyTester {

  private int      listSize  = 0;
  private List     trades    = new ArrayList();
  private Shuffler shuffler;
  private List     clientSet = new ArrayList();

  public ConcurrencyTester(int listSize) {
    initializeList(listSize);
    shuffler = new Shuffler(listSize);
  }

  private void initializeList(int size) {
    listSize = size;
    synchronized (trades) {
      if (trades.size() != listSize) {
        trades.clear();
        for (int i = 0; i < listSize; i++) {
          trades.add(new SimpleTrade());
        }
      }
    }
  }

  private void clear() {
    synchronized (trades) {
      trades.clear();
    }
  }

  public void awaitClients(int numClients, boolean verbose) {
    synchronized (clientSet) {
      clientSet.add(new Object());
      clientSet.notifyAll();
      while (clientSet.size() != numClients && clientSet.size() != 0) {
        if (verbose) {
          System.out.println("Waiting for " + (numClients - clientSet.size()) + " to start...");
        }
        try {
          clientSet.wait();
        } catch (InterruptedException ie) {
          // Ignore
        }
      }
      clientSet.clear();
    }
  }

  public void incrementInRandomOrder() {
    int[] incrementOrder = shuffler.shuffleOrder();
    for (int i = 0; i < incrementOrder.length; i++) {
      SimpleTrade st;
      synchronized (trades) {
        st = (SimpleTrade) trades.get(incrementOrder[i]);
      }
      st.incrementCounter();
    }
  }

  public static void main(String[] args) {
    if (args.length != 3) {
      System.out.println("usage: java dso.concurrency.ConcurrencyTester <rounds> <listsize> <numclients>");
      System.exit(1);
    }
    int rounds = Integer.parseInt(args[0]);
    int listSize = Integer.parseInt(args[1]);
    int numClients = Integer.parseInt(args[2]);

    ConcurrencyTester ct = new ConcurrencyTester(listSize);
    ct.awaitClients(numClients, true);
    ct.updateValues(rounds);
    ct.awaitClients(numClients, false);
    ct.printResults();
    ct.awaitClients(numClients, false);
    ct.clear();
  }

  private synchronized void printResults() {
    for (int i = 0; i < listSize; i++) {
      System.out.println("Final Value of position " + i + " is " + ((SimpleTrade) trades.get(i)).getCounter());
    }
  }

  private void updateValues(int rounds) {
    for (int i = 0; i < rounds; i++) {
      incrementInRandomOrder();
    }
  }

}
