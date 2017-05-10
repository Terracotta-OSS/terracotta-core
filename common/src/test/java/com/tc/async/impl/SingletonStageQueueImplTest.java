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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author mscott
 */
public class SingletonStageQueueImplTest {


  public SingletonStageQueueImplTest() {
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
  public void testBasicQueuing() {
    int index = 0;
    TCLoggerProvider logger = new DefaultLoggerProvider();
    final List<BlockingQueue<Object>> cxts = new ArrayList<BlockingQueue<Object>>();

    QueueFactory<ContextWrapper<Object>> context = mock(QueueFactory.class);
    when(context.createInstance(Matchers.anyInt())).thenAnswer(new Answer<BlockingQueue<Object>>() {

      @Override
      public BlockingQueue<Object> answer(InvocationOnMock invocation) throws Throwable {
        BlockingQueue<Object> queue = new ArrayBlockingQueue<Object>((Integer) invocation.getArguments()[0]);
        cxts.add(queue);
        return queue;
      }

    });
    StageQueue<Object> instance = new SingletonStageQueueImpl(context, logger, "mock", 16);
    assertEquals(cxts.size(), 1);
    for (int x = 0; x < cxts.size(); x++) {
      assertNotNull(instance.getSource(index));
    }
    assertNull(instance.getSource(cxts.size()));

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
    for (int x = 0; x < 2; x++) {
      instance.addMultiThreaded(context1);
    }
    for (Queue<Object> q : cxts) {
      assertThat(q.size(), org.hamcrest.Matchers.lessThanOrEqualTo(2));
      q.clear();
    }

    System.out.println("test specific queue");
    when(context1.getSchedulingKey()).thenReturn(1);
    instance.addMultiThreaded(context1);
    //  everything should hash to 0
    for (int x = 0; x < cxts.size(); x++) {
      if (x != 0) {
        assertTrue(cxts.get(x).isEmpty());
      } else {
        assertEquals(cxts.get(x).poll(), context1);
      }
    }

    int rand = (int) (Math.random() * Integer.MAX_VALUE);
    when(context1.getSchedulingKey()).thenReturn(rand);
    instance.addMultiThreaded(context1);
    //  tests specific implementation.  test expectation
    assertEquals(cxts.get(rand % cxts.size()).poll(), context1);
  }

}
