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

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.logging.DefaultLoggerProvider;
import com.tc.logging.TCLoggerProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.QueueFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.mockito.Matchers;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 *
 * @author mscott
 */
public class StageQueueImplTest {
  
  public StageQueueImplTest() {
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

  /**
   * Test of getSource method, of class StageQueueImpl.
   */
  @Test
  public void testBasicMultiContext() {
    System.out.println("multi context");
    int index = 0;
    int size = 4;
    TCLoggerProvider logger = new DefaultLoggerProvider();
    final List<BlockingQueue<Object>> cxts = new ArrayList<BlockingQueue<Object>>();
    
    QueueFactory<ContextWrapper<Object>> context = mock(QueueFactory.class);
    when(context.createInstance(Matchers.anyInt())).thenAnswer(new Answer<BlockingQueue<Object>>() {

      @Override
      public BlockingQueue<Object> answer(InvocationOnMock invocation) throws Throwable {
        BlockingQueue<Object> queue = new ArrayBlockingQueue<Object>((Integer)invocation.getArguments()[0]);
        cxts.add(queue);
        return queue;
      }
    
    });
    StageQueueImpl<Object> instance = new StageQueueImpl<Object>(size, context, logger, "mock", 16);
    for (int x=0;x<cxts.size();x++) {
      assertNotNull(instance.getSource(index));
    }
    assertNull(instance.getSource(cxts.size()));
    assertEquals(cxts.size(), size);
    
    MultiThreadedEventContext context1 = mock(MultiThreadedEventContext.class);
    when(context1.getSchedulingKey()).thenReturn(null);
    System.out.println("test add");
    instance.addMultiThreaded(context1);
    boolean found = false;
    for (Queue<Object> q : cxts) {
      if (q.poll() != null) {
        found = true;
      }
    }
    assertTrue(found);
    System.out.println("test even distribution with no key");
    for (int x=0;x<size*2;x++) {
      instance.addMultiThreaded(context1);
    }
    for (Queue<Object> q : cxts) {
      assertThat(q.size(), org.hamcrest.Matchers.lessThanOrEqualTo(2));
      q.clear();
    }
    
    System.out.println("test specific queue");
    when(context1.getSchedulingKey()).thenReturn(1);
    instance.addMultiThreaded(context1);
//  int should hash to int
    for (int x=0;x<cxts.size();x++) {
      if (x != 1) {
        assertTrue(cxts.get(x).isEmpty());
      } else {
        assertEquals(cxts.get(x).poll(), context1);
      }
    }
    
    int rand = (int)(Math.random() * Integer.MAX_VALUE);
    when(context1.getSchedulingKey()).thenReturn(rand);
    instance.addMultiThreaded(context1);
//  tests specific implementation.  test expectation
    assertEquals(cxts.get(rand % cxts.size()).poll(), context1);
  }
  
  @Test
  public void testShortestRollover() throws Exception {
    TCLoggerProvider logger = new DefaultLoggerProvider();
    final List<BlockingQueue<Object>> cxts = new ArrayList<BlockingQueue<Object>>();
    
    QueueFactory<ContextWrapper<Object>> context = mock(QueueFactory.class);
    when(context.createInstance(Matchers.anyInt())).thenAnswer(new Answer<BlockingQueue<Object>>() {

      @Override
      public BlockingQueue<Object> answer(InvocationOnMock invocation) throws Throwable {
        BlockingQueue<Object> queue = new ArrayBlockingQueue<Object>((Integer)invocation.getArguments()[0]);
        cxts.add(queue);
        return queue;
      }
    
    });
    StageQueueImpl impl = new StageQueueImpl(6, context, logger, "mock", 16);
    MultiThreadedEventContext cxt = mock(MultiThreadedEventContext.class);
    when(cxt.getSchedulingKey()).thenReturn(null);
 // fcheck starts at zero and should stay at zero because first queue is always empty
    for (int x=0;x<6;x++) {
      Assert.assertTrue(impl.getSource(0).isEmpty());
      impl.addMultiThreaded(cxt);
      Assert.assertNotNull(impl.getSource(0).poll(0));
    }
//  now try and fill one each on the the queues
    for (int x=0;x<6;x++) {
      Assert.assertTrue(impl.getSource(x).isEmpty());
      impl.addMultiThreaded(cxt);
      Assert.assertFalse(cxts.get(x).isEmpty());
    }
//  now clear the last three and re-fill them
    for (int x=3;x<6;x++) {
      Assert.assertFalse(impl.getSource(x).isEmpty());
      Assert.assertNotNull(impl.getSource(x).poll(0));
      Assert.assertTrue(cxts.get(x).isEmpty());
      impl.addMultiThreaded(cxt);
      Assert.assertFalse(cxts.get(x).isEmpty());
    }
//  now clear all again
    for (int x=0;x<6;x++) {
      Assert.assertFalse(impl.getSource(x).isEmpty());
      Assert.assertNotNull(impl.getSource(x).poll(0));
      Assert.assertTrue(cxts.get(x).isEmpty());
    }
// now add one more and make sure it is at the last queue since that was the 
// last to be cleared
    impl.addMultiThreaded(cxt);
    Assert.assertFalse(cxts.get(5).isEmpty());
  }
}
