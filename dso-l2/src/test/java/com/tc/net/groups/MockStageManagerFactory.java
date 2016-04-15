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
package com.tc.net.groups;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.logging.TCLogger;
import static java.lang.Thread.State.RUNNABLE;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.mockito.Matchers;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class MockStageManagerFactory {
  
  private final ThreadGroup threadGroup;
  private final TCLogger logging;
  private volatile boolean alive = true;

  public MockStageManagerFactory(TCLogger logging, ThreadGroup group) {
    this.threadGroup = group;
    this.logging = logging;
  }

  public StageManager createStageManager() throws Exception {
    StageManager stages = mock(StageManager.class);
    ConcurrentHashMap<String, Stage> created = new ConcurrentHashMap<>();
    when(stages.createStage(Matchers.anyString(), Matchers.any(), Matchers.any(), Matchers.anyInt(), Matchers.anyInt()))
      .then((invoke)->{
        String stageName = invoke.getArguments()[0].toString();
        int size = (Integer)invoke.getArguments()[4];
        ExecutorService service = createExecutor(stageName, size);
        Stage stage = mock(Stage.class);
        Sink sink = mock(Sink.class);
        EventHandler ev = (EventHandler)invoke.getArguments()[2];
        doAnswer((invoke2)->{
          service.submit(()->{
            try {
              ev.handleEvent(invoke2.getArguments()[0]);
            } catch (EventHandlerException e) {
            }
          });
          return null;
        }).when(sink).addSingleThreaded(Matchers.any());
        
        doAnswer((invoke2)->{
          service.submit(()->{
            try {
              ev.handleEvent(invoke2.getArguments()[0]);
            } catch (EventHandlerException e) {
            }
          });
          return null;
        }).when(sink).addMultiThreaded(Matchers.any());
        
        when(stage.getSink()).thenReturn(sink);
        created.put(stageName, stage);
        return stage;
      });   
    
    when(stages.getStage(Matchers.anyString(), Matchers.any()))
        .then((invoke)->{
          return created.get(invoke.getArguments()[0].toString());
        });
    return stages;
  }
  
  private ExecutorService createExecutor(String name, int size) {
    BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(size);
    new Thread(threadGroup, ()->{ 
      
      while (alive) {
        Runnable next = null;
        try {
          next = queue.take();
        } catch (InterruptedException ie) {
          break;
        } 
        if (next != null) next.run();
      }
      
      queue.clear();
    }, "Stage - " + name).start();
    
    return new AbstractExecutorService() {
      @Override
      public void shutdown() {
       
      }

      @Override
      public List<Runnable> shutdownNow() {
        return Collections.emptyList();
      }

      @Override
      public boolean isShutdown() {
        return !alive;
      }

      @Override
      public boolean isTerminated() {
        return threadGroup.activeCount() == 0;
      }

      @Override
      public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
      }

      @Override
      public void execute(Runnable command) {
        try {
          queue.put(command);
        } catch (InterruptedException ie) {
          throw new RuntimeException(ie);
        }
      }
    };
  }
  
  public static <T> EventHandler<T> createEventHandler(Consumer<T> r) {
    return new AbstractEventHandler<T>() {
      @Override
      public void handleEvent(T context) throws EventHandlerException {
        r.accept(context);
      }
    };
  }
  
  public void quietThreads() {
    Thread[] threads = new Thread[250];
    int spins = 0;
    boolean waited = true;
    while (waited && spins++ < 5 && threadGroup.activeCount() > 0) {
      waited = false;
      int count = threadGroup.enumerate(threads);
      for (int x=0;x<count;x++) {
        try {
          if (threads[x].isAlive()) {
          Thread.State state = threads[x].getState();
            if (state == RUNNABLE) {
              Thread.sleep(1000);
              logging.info(threads[x].getName() + " is RUNNABLE, sleeping 1 sec.");
              waited = true;
            } else {
              logging.info(threads[x].getName() + " is " + state);
            }
          }
        } catch (InterruptedException ie) {
          throw new RuntimeException(ie);
        }
      }
    }
  }
  
  public void shutdown() {
    alive = false;
    Thread[] threads = new Thread[250];
    while (threadGroup.activeCount() > 0) {
      int count = threadGroup.enumerate(threads);
      for (int x=0;x<count;x++) {
        try {
          threads[x].interrupt();
          threads[x].join();
        } catch (InterruptedException ie) {
          throw new RuntimeException(ie);
        }
      }
    }
  }
}
