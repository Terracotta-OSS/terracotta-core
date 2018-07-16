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

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.MultiThreadedEventContext;
import com.tc.logging.DefaultLoggerProvider;
import com.tc.logging.TCLoggerProvider;
import com.tc.util.concurrent.QueueFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import org.junit.Assert;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author mscott
 */
public class StageImplTest {
  
  public StageImplTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
  }
  
  @After
  public void tearDown() {
  }
  
  @Test
  public void testFlushOfVariousSizes() throws Exception {
    testMultiContextFlush(1);
    testMultiContextFlush(4);
    testMultiContextFlush(12);
  }
  
  @Test
  public void testRapidTeardown() throws Exception {
    TCLoggerProvider logger = new DefaultLoggerProvider();
    QueueFactory context = mock(QueueFactory.class);
    when(context.createInstance(Matchers.any())).thenReturn(new ArrayBlockingQueue<>(16));
    when(context.createInstance(Matchers.any(), anyInt())).thenReturn(new ArrayBlockingQueue<>(16));
    EventHandler handler = mock(EventHandler.class);

    StageImpl<Object> instance = new StageImpl<Object>(logger, "mock", Object.class, handler, 1, null, context, 16, false);
    instance.destroy();
    verify(handler, never()).destroy();
    
    instance.start(null);
    verify(handler).initializeContext(Mockito.any(ConfigurationContext.class));
    instance.start(null);
    verify(handler).initializeContext(Mockito.any(ConfigurationContext.class));
    instance.destroy();
    verify(handler).destroy();
    instance.destroy();
    verify(handler).destroy();
    
  }
  
  @Test
  public void testSingletonClear() throws Exception {
    TCLoggerProvider logger = new DefaultLoggerProvider();
    QueueFactory context = mock(QueueFactory.class);
    ArrayBlockingQueue queue = new ArrayBlockingQueue<>(16);
    when(context.createInstance(Matchers.any())).thenReturn(queue);
    when(context.createInstance(Matchers.any(), anyInt())).thenReturn(queue);
    EventHandler handler = mock(EventHandler.class);

    StageImpl<Object> instance = new StageImpl<Object>(logger, "mock", Object.class, handler, 1, null, context, 16, false);
    Object event = new Object();
    instance.getSink().addToSink(context);
    instance.getSink().addToSink(context);
    instance.getSink().addToSink(context);
    instance.getSink().addToSink(context);
    
    Assert.assertEquals(4, instance.size());
    
    instance.clear();
    
    Assert.assertEquals(0, instance.size());
    Assert.assertTrue(instance.isEmpty());
  }
  
  @Test
  public void testMultipleClear() throws Exception {
    TCLoggerProvider logger = new DefaultLoggerProvider();
    QueueFactory context = mock(QueueFactory.class);
    ArrayBlockingQueue queue = new ArrayBlockingQueue<>(16);
    when(context.createInstance(Matchers.any())).thenReturn(queue);
    when(context.createInstance(Matchers.any(), anyInt())).thenReturn(queue);
    EventHandler handler = mock(EventHandler.class);

    StageImpl<MultiThreadedEventContext> instance = new StageImpl<>(logger, "mock", MultiThreadedEventContext.class, handler, 4, null, context, 16, false);
    MultiThreadedEventContext event = new MultiThreadedEventContext() {
      @Override
      public Object getSchedulingKey() {
        return null;
      }

      @Override
      public boolean flush() {
        return false;
      }

    };
    instance.getSink().addToSink(event);
    instance.getSink().addToSink(event);
    instance.getSink().addToSink(event);
    instance.getSink().addToSink(event);
    
    Assert.assertEquals(4, instance.size());
    
    instance.clear();
    
    Assert.assertEquals(0, instance.size());
    Assert.assertTrue(instance.isEmpty());
  }
  
  private void testMultiContextFlush(int size) throws Exception {
    System.out.println("test a multi context flush");
    TCLoggerProvider logger = new DefaultLoggerProvider();
    final List<BlockingQueue<Object>> cxts = new ArrayList<BlockingQueue<Object>>();
//  barrier on when the context is actually executed
    final CyclicBarrier barrier = new CyclicBarrier(2);
    EventHandler handler = new EventHandler() {
      @Override
      public void handleEvent(Object context) throws EventHandlerException {
        try {
          barrier.await();
        } catch (BrokenBarrierException bb) {
        } catch (InterruptedException in) {
        }
      }
      @Override
      public void handleEvents(Collection context) throws EventHandlerException { }
      @Override
      public void destroy() { }
      @Override
      public void initializeContext(ConfigurationContext context) { }
    };
    
    QueueFactory context = mock(QueueFactory.class);
    when(context.createInstance(Matchers.any(), Matchers.anyInt())).thenAnswer(new Answer<BlockingQueue<Object>>() {

      @Override
      public BlockingQueue<Object> answer(InvocationOnMock invocation) throws Throwable {
//  spy each call to put of the queue to make sure each queue is getting hit.
        BlockingQueue<Object> queue = Mockito.spy(new ArrayBlockingQueue<Object>((Integer)invocation.getArguments()[1]));
        cxts.add(queue);
        return queue;
      }
    
    });
    StageImpl<MultiThreadedEventContext> instance = new StageImpl<>(logger, "mock", MultiThreadedEventContext.class, handler, size, null, context, 16, false);
    assertEquals(cxts.size(), size);
    instance.start(null);
    
    MultiThreadedEventContext cxt = mock(MultiThreadedEventContext.class);
    when(cxt.flush()).thenReturn(Boolean.TRUE);
    when(cxt.getSchedulingKey()).thenReturn(1);
    
    instance.getSink().addToSink(cxt);
    if (size > 1) {
      // if size is one, this will not be called.
      verify(cxt).getSchedulingKey();
      verify(cxt).flush();
    }

    barrier.await();
    for (BlockingQueue q : cxts) {
      verify(q).put(Matchers.any(MultiThreadedEventContext.class));
    }
    
  }
}
