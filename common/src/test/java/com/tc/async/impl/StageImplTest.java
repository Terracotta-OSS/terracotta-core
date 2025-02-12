/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
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
    when(context.createInstance(ArgumentMatchers.any(), anyInt())).thenReturn(new ArrayBlockingQueue<>(16));
    EventHandler handler = mock(EventHandler.class);

    StageImpl<Object> instance = new StageImpl<Object>(logger, "mock", Object.class, handler, 1, null, context, null, 16, false, true);
    instance.destroy();
    verify(handler, never()).destroy();
    
    instance.start(mock(ConfigurationContext.class));
    verify(handler).initializeContext(Mockito.<ConfigurationContext>any());
    instance.start(mock(ConfigurationContext.class));
    verify(handler).initializeContext(Mockito.<ConfigurationContext>any());
    instance.destroy();
    verify(handler).destroy();
    instance.destroy();
    verify(handler).destroy();
    
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
    when(context.createInstance(ArgumentMatchers.any(), ArgumentMatchers.anyInt())).thenAnswer((InvocationOnMock invocation) -> {
      //  spy each call to put of the queue to make sure each queue is getting hit.
      BlockingQueue<Object> queue = Mockito.spy(new ArrayBlockingQueue<>((Integer)invocation.getArguments()[1]));
      cxts.add(queue);
      return queue;
    });
    StageImpl<MultiThreadedEventContext> instance = new StageImpl<>(logger, "mock", MultiThreadedEventContext.class, handler, size, null, context, null, 16, false, true);
    assertEquals(cxts.size(), size);
    instance.start(mock(ConfigurationContext.class));
    
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
      verify(q).put(Mockito.<MultiThreadedEventContext>any());
    }
    
  }
}
