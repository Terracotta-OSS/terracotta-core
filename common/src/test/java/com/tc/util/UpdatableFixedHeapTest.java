package com.tc.util;

import com.tc.util.UpdatableFixedHeap.UpdatableWrapper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by cschanck on 5/1/2017.
 */
public class UpdatableFixedHeapTest {


  @Test
  public void testPopulate() throws Exception {
    Random rand = new Random(0);

    int MANY = 20;

    MutableCounter[] arr = new MutableCounter[MANY];

    for (int i = 0; i < MANY; i++) {
      arr[i] = new MutableCounter(rand.nextInt(500));
    }

    UpdatableFixedHeap<MutableCounter> heap = new UpdatableFixedHeap<MutableCounter>(true, arr);

    heap.verify();
  }

  @Test
  public void testUpdate1Threads() throws Exception {
    Random rand = new Random(0);

    int MANY = 20;

    MutableCounter[] arr = new MutableCounter[MANY];

    for (int i = 0; i < MANY; i++) {
      arr[i] = new MutableCounter(rand.nextInt(500));
    }

    UpdatableFixedHeap<MutableCounter> heap = new UpdatableFixedHeap<MutableCounter>(true, arr);

    Random r = new Random(0);
    for (int iter = 0; iter < 500; iter++) {
      System.out.println(iter);
      boolean bigger = r.nextBoolean();
      int index = r.nextInt(heap.getSize());
      UpdatableWrapper<MutableCounter> probe = heap.wrapperFor(index);
      MutableCounter p = probe.getPayload();
      if (bigger) {
        p.increment();
        heap.possiblyCloserToFront(probe);
      } else {
        p.decrement();
        heap.possiblyCloserToEnd(probe);
      }
      heap.verify();
    }
    System.out.println(heap);
    heap.verify();
  }

  @Test
  public void test4Threads() throws Exception {
    nThreads(4, 20);
  }

  private void nThreads(int nThreads, int many) throws Exception {
    Random rand = new Random(0);

    MutableCounter[] arr = new MutableCounter[many];

    for (int i = 0; i < many; i++) {
      arr[i] = new MutableCounter(rand.nextInt(many));
    }

    final UpdatableFixedHeap<MutableCounter> heap = new UpdatableFixedHeap<MutableCounter>(true, arr);
    System.out.println("Before: " + heap);

    final AtomicBoolean die = new AtomicBoolean(false);
    ArrayList<Thread> threads = new ArrayList<Thread>();
    for (int i = 0; i < nThreads; i++) {
      final int finalI = i;
      Thread t = new Thread() {
        @Override
        public void run() {
          Random r = new Random(finalI);
          for (; !die.get(); ) {
            boolean bigger = r.nextBoolean();
            int index = r.nextInt(heap.getSize());
            try {
              Thread.sleep(r.nextInt(50));
            } catch (InterruptedException e) {

            }
            UpdatableWrapper<MutableCounter> probe = heap.wrapperFor(index);
            MutableCounter p = probe.getPayload();
            if (bigger) {
              p.increment(3);
              heap.possiblyCloserToFront(probe);
            } else {
              p.decrement(3);
              heap.possiblyCloserToEnd(probe);
            }
          }
        }
      };
      threads.add(t);
    }
    Thread.sleep(3 * 1000);
    die.set(true);
    for (Thread t : threads) {
      t.join();
    }
    System.out.println("After: " + heap);
    heap.verify();
  }

  static class MutableCounter implements UpdatableFixedHeap.Ordered {
    private int val = 0;

    public MutableCounter(int val) {
      this.val = val;
    }

    public int getValue() {
      return val;
    }

    public MutableCounter increment() {
      val++;
      return this;
    }

    public MutableCounter decrement() {
      val--;
      return this;
    }

    @Override
    public String toString() {
      return "MutatableCounter{" + "val=" + val + '}';
    }

    public MutableCounter decrement(int i) {
      val = val - i;
      return this;
    }

    public MutableCounter increment(int i) {
      val = val + i;
      return this;
    }

    @Override
    public int getHeapOrderingAmount() {
      return val;
    }
  }
}