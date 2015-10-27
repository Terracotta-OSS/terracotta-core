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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.Matchers;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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
    
    QueueFactory<ContextWrapper<Object>> context = mock(QueueFactory.class);
    when(context.createInstance(Matchers.anyInt())).thenAnswer(new Answer<BlockingQueue<Object>>() {

      @Override
      public BlockingQueue<Object> answer(InvocationOnMock invocation) throws Throwable {
//  spy each call to put of the queue to make sure each queue is getting hit.
        BlockingQueue<Object> queue = Mockito.spy(new ArrayBlockingQueue<Object>((Integer)invocation.getArguments()[0]));
        cxts.add(queue);
        return queue;
      }
    
    });
    StageImpl<Object> instance = new StageImpl<Object>(logger, "mock", handler, size, null, context, 16);
    assertEquals(cxts.size(), size);
    instance.start(null);
    
    MultiThreadedEventContext cxt = mock(MultiThreadedEventContext.class);
    when(cxt.flush()).thenReturn(Boolean.TRUE);
    
    instance.getSink().addMultiThreaded(cxt);
    verify(cxt).getSchedulingKey();
    verify(cxt).flush();
    
    barrier.await();
    for (BlockingQueue q : cxts) {
      verify(q).put(Matchers.any(MultiThreadedEventContext.class));
    }
    
  }
}
