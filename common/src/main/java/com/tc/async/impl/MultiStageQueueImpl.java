/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.async.impl;

import com.tc.async.api.EventHandlerException;
import com.tc.async.api.MultiThreadedEventContext;
import com.tc.async.api.Source;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLoggerProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.QueueFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * This StageQueueImpl represents the sink and gives a handle to the source. We are internally just using a queue
 * since our queues are locally processed. This class can be replaced with a distributed queue to enable processing
 * across process boundaries.
 */
public class MultiStageQueueImpl<EC extends MultiThreadedEventContext> extends AbstractStageQueueImpl<EC> {

  static final String FINDSTRATEGY_PROPNAME = "tc.stagequeueimpl.findstrategy";


  static final ShortestFindStrategy SHORTEST_FIND_STRATEGY;

  static {
    ShortestFindStrategy strat = ShortestFindStrategy.PARTITION;
    try {
      strat = chooseStrategy(ShortestFindStrategy.PARTITION);
    } catch (Throwable t) {
    }
    SHORTEST_FIND_STRATEGY = strat;
  }


  static enum ShortestFindStrategy {
    BRUTE,
    PARTITION
  }

  private final boolean moduloAnd;
  private final int moduleMask;
  private final int PARTITION_SHIFT;
  final int PARTITION_MAX_MASK;
  private final MultiSourceQueueImpl[] sourceQueues;
  private volatile int fcheck = 0;  // used to start the shortest queue search
  AtomicInteger partitionHand =new AtomicInteger(0);

  /**
   * The Constructor.
   *
   * @param queueCount : Number of queues working on this stage
   * @param queueFactory : Factory used to create the queues
   * @param loggerProvider : logger
   * @param stageName : The stage name
   * @param queueSize : Max queue Size allowed
   */
  @SuppressWarnings("unchecked")
  MultiStageQueueImpl(int queueCount,
                      QueueFactory queueFactory,
                      Class<EC> type, 
                      EventCreator<EC> creator,
                      TCLoggerProvider loggerProvider,
                      String stageName,
                      int queueSize) {
    super(loggerProvider, stageName, creator);
    Assert.eval(queueCount > 0);

    if (queueCount >= 8) {
      PARTITION_SHIFT = 2;
    } else {
      PARTITION_SHIFT = 1;
    }
    PARTITION_MAX_MASK = (1 << (31 - PARTITION_SHIFT)) - 1;
    this.sourceQueues = new MultiSourceQueueImpl[queueCount];
    createWorkerQueues(queueCount, queueFactory, type, queueSize, stageName);

    if (Integer.bitCount(queueCount) == 1) {
      this.moduloAnd = true;
      this.moduleMask = queueCount - 1;
    } else {
      this.moduloAnd = false;
      this.moduleMask = 0;
    }
  }

  @Override
  SourceQueue[] getSources() {
    return this.sourceQueues;
  }  

  private static ShortestFindStrategy chooseStrategy(ShortestFindStrategy defaultVal) {
    String stratName = System.getProperty(FINDSTRATEGY_PROPNAME, defaultVal.name());
    for (ShortestFindStrategy s : ShortestFindStrategy.values()) {
      if (s.name().toUpperCase().equals(stratName.toUpperCase())) {
        return s;
      }
    }
    System.err.println("Unrecognized '" + FINDSTRATEGY_PROPNAME + "' value: " + stratName + "; using: " + defaultVal);
    return defaultVal;
  }

  private int moduloQueueCount(int i) {
    if (moduloAnd) {
      return i & moduleMask;
    } else {
      return i % this.sourceQueues.length;
    }
  }

  private void createWorkerQueues(int queueCount, QueueFactory queueFactory, Class<EC> type, int queueSize, String stage) {
    if (queueSize != Integer.MAX_VALUE) {
      queueSize = (int) Math.ceil(((double) queueSize) / queueCount);
    }
    Assert.eval(queueSize > 0);

    for (int i = 0; i < queueCount; i++) {
      this.sourceQueues[i] = new MultiSourceQueueImpl(queueFactory.createInstance(type, queueSize), v->this.fcheck = v, i);
    }
  }

  @Override
  public Source getSource(int index) {
    return (index < 0 || index >= this.sourceQueues.length) ? null : this.sourceQueues[index];
  }

  @Override
  public void addToSink(EC context) {
    Assert.assertNotNull(context);
    if (isClosed()) {
      throw new IllegalStateException("closed");
    }
    if (this.logger.isDebugEnabled()) {
      this.logger.debug("Added:" + context + " to:" + this.stageName);
    }
    Event event = createEvent(context);
    if (event != null) {
      // NOTE:  We don't currently consult the predicate for multi-threaded events (the only implementation always returns true, in any case).
      boolean interrupted = Thread.interrupted();
      int index = getSourceQueueFor(context);
      Event wrapper = (context.flush()) ? new FlushingHandledContext(event, index) : event;
      try {
        while (true) {
          try {
            this.sourceQueues[index].put(wrapper);
            break;
          } catch (InterruptedException e) {
            this.logger.debug("StageQueue Add: " + e);
            interrupted = true;
          }
        }
      } finally {
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  // TODO:  Way too busy. REALLY need a better way
  private int findShortestQueueIndex() {
    switch (SHORTEST_FIND_STRATEGY) {
      case PARTITION: {
        int offset = moduloQueueCount(nextPartition() << PARTITION_SHIFT);
        int min = Integer.MAX_VALUE;
        int can = -1;
        for (int i = 0; i < (1 << PARTITION_SHIFT); i++) {
          int checkMin = this.sourceQueues[offset].size();
          if (checkMin < min) {
            can = offset;
            min = checkMin;
          }
          offset = moduloQueueCount(offset + 1);
        }
        return can;
      }
      case BRUTE: {
        final int pointer = fcheck;
        int min = Integer.MAX_VALUE;
        int can = -1;
        // special case where context can go to any queue, pick the shortest
        for (int x = 0; x < this.sourceQueues.length; x++) {
          int index = moduloQueueCount(pointer + x);
          MultiSourceQueueImpl impl = this.sourceQueues[index];
          if (impl.isEmpty()) {
            return index;
          } else {
            int checkMin = impl.size(); // concurrent access so just use current value.  this method is best efforts
            if (Math.min(min, checkMin) != min) {
              can = index;
              min = checkMin;
            }
          }
        }
        Assert.assertTrue(can >= 0 && can < this.sourceQueues.length);
        return can;
      }
    }
    throw new IllegalStateException();
  }

  private int nextPartition() {
    int p = partitionHand.get();
    int newP = (p + 1) & PARTITION_MAX_MASK;
    while(!partitionHand.compareAndSet(p, newP)) {
      p = partitionHand.get();
      newP = (p + 1) & PARTITION_MAX_MASK;
    } return newP;
  }

  private int getSourceQueueFor(EC context) {
    EC multi = context;
    Object schedulingKey = multi.getSchedulingKey();
    if (null == schedulingKey) {
      return findShortestQueueIndex();
    } else {
      return hashCodeToArrayIndex(schedulingKey.hashCode(), this.sourceQueues.length);
    }
  }

  private int hashCodeToArrayIndex(int hashcode, int arrayLength) {
    return Math.abs(hashcode % arrayLength);
  }
  
  @Override
  public String toString() {
    return "StageQueue(" + this.stageName + ")";
  }

  @Override
  public int clear() {
    int clearCount = 0;
    for (MultiSourceQueueImpl sourceQueue : this.sourceQueues) {
      clearCount += sourceQueue.clear();
    }
    this.logger.info("Cleared " + clearCount);
    return clearCount;
  }

  private static final class MultiSourceQueueImpl implements SourceQueue {

    private final Consumer<Integer> hint;
    private final BlockingQueue<Event> queue;
    private final int                      sourceIndex;

    public MultiSourceQueueImpl(BlockingQueue<Event> queue, Consumer<Integer> hint, int sourceIndex) {
      this.queue = queue;
      this.hint = hint;
      this.sourceIndex = sourceIndex;
    }

    @Override
    public String toString() {
      return "SourceQueueImpl{" + sourceIndex + "size=" + queue.size() + '}';
    }

    // XXX: poor man's clear.
    @Override
    public int clear() {
      int cleared = 0;
      try {
        while (poll(0) != null) {
          cleared++;
        }
        return cleared;
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
    }

    @Override
    public boolean isEmpty() {
      return this.queue.isEmpty();
    }

    @Override
    public Event poll(long timeout) throws InterruptedException {
      Event rv = this.queue.poll(timeout, TimeUnit.MILLISECONDS);
      if (rv != null) {
        if (queue.isEmpty()) {
          // set the empty index for shortest queue in hopes of catching it on the first try
          hint.accept(this.sourceIndex);
        }
      } else {
        hint.accept(this.sourceIndex);
      }
      return rv;
    }

    @Override
    public void put(Event context) throws InterruptedException {
      this.queue.put(context);
    }

    @Override
    public int size() {
      return this.queue.size();
    }

    @Override
    public String getSourceName() {
      return Integer.toString(this.sourceIndex);
    }

  }

  private class FlushingHandledContext<T extends EC> extends HandledEvent<EC> {
    private final int offset;
    private int executionCount = 0;
    public FlushingHandledContext(Event context, int offset) {
      super(context);
      this.offset = offset;
    }
    
    @Override
    public void call() throws EventHandlerException {
      if (++executionCount == sourceQueues.length) {
//  been through all the queues.  execute now.
        super.call();
      } else {
//  move to next queue
        boolean interrupted = false;
        try {
          while (true) {
            try {
              sourceQueues[moduloQueueCount(executionCount + offset)].put(this);
              break;
            } catch (InterruptedException e) {
              logger.debug("FlushingHandledContext move to next queue: " + e + " : " + ((executionCount + offset) % sourceQueues.length));
              interrupted = true;
            }
          }
        } finally {
          if (interrupted) {
            Thread.currentThread().interrupt();
          }
        }
      }
    }
  }
}
