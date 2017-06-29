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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.async.api.Stage;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandlerImpl;
import com.tc.util.concurrent.QueueFactory;

import junit.framework.TestCase;

/**
 * @author steve
 */
public class StageManagerImplTest extends TestCase {
  private static final Logger logging = LoggerFactory.getLogger(StageManagerImplTest.class);
  static {
    logging.info("I have to load this class for breaking circular dependency");
  }

  private StageManagerImpl stageManager;
  private TestEventHandler<TestEventContext> testEventHandler;
  private StageManagerImpl multiThreadedStageManager;
  private TestEventHandler<TestMultiThreadedEventContext> multiThreadedTestEventHandler;

  /**
   * Constructor for StageManagerImplTest.
   * 
   * @param arg0
   */
  public StageManagerImplTest(String arg0) {
    super(arg0);
  }

  public static void main(String[] args) {
    //
  }

  /*
   * @see TestCase#setUp()
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
      stageManager = new StageManagerImpl(new TCThreadGroup(new ThrowableHandlerImpl(LoggerFactory.getLogger(StageManagerImpl.class))), new QueueFactory<TestEventContext>());
      testEventHandler = new TestEventHandler<TestEventContext>();
      multiThreadedStageManager = new StageManagerImpl(new TCThreadGroup(new ThrowableHandlerImpl(LoggerFactory.getLogger(StageManagerImpl.class))), new QueueFactory<TestMultiThreadedEventContext>());
      multiThreadedTestEventHandler = new TestEventHandler<TestMultiThreadedEventContext>();
  }

  public void testStage() throws Exception {
    stageManager.createStage("testStage", TestEventContext.class, testEventHandler, 1, 3);
    Stage<TestEventContext> s = stageManager.getStage("testStage", TestEventContext.class);
    assertTrue(s != null);
    s.getSink().addSingleThreaded(new TestEventContext());
    assertTrue(s.getSink().size() == 1);
    assertTrue(testEventHandler.getContexts().size() == 0);
    s.getSink().addSingleThreaded(new TestEventContext());
    assertTrue(s.getSink().size() == 2);
    assertTrue(testEventHandler.getContexts().size() == 0);
    s.start(new ConfigurationContextImpl(null));
    testEventHandler.waitForEventContextCount(2, 60, SECONDS);
    assertTrue(s.getSink().size() == 0);
    assertTrue(testEventHandler.getContexts().size() == 2);
    stageManager.stopAll();
  }

  public void testMultiThreadedStage() throws Exception {
    stageManager.createStage("testStage2", TestEventContext.class, testEventHandler, 3, 30);
    Stage<TestEventContext> s = stageManager.getStage("testStage2", TestEventContext.class);
    assertTrue(s != null);
    s.getSink().addSingleThreaded(new TestEventContext());
    s.getSink().addSingleThreaded(new TestEventContext());
    s.getSink().addSingleThreaded(new TestEventContext());
    s.getSink().addSingleThreaded(new TestEventContext());
    assertTrue(s.getSink().size() == 4);
    assertTrue(multiThreadedTestEventHandler.getContexts().size() == 0);

    s.getSink().addSingleThreaded(new TestEventContext());
    s.getSink().addSingleThreaded(new TestEventContext());
    s.getSink().addSingleThreaded(new TestEventContext());
    s.getSink().addSingleThreaded(new TestEventContext());
    assertTrue(s.getSink().size() == 8);
    assertTrue(testEventHandler.getContexts().size() == 0);

    s.start(new ConfigurationContextImpl(null));
    testEventHandler.waitForEventContextCount(8, 60, SECONDS);
    assertTrue(s.getSink().size() == 0);
    assertTrue(testEventHandler.getContexts().size() == 8);
    stageManager.stopAll();
  }

  public void testMultiThreadedContext() throws Exception {
    multiThreadedStageManager.createStage("testStage2", TestMultiThreadedEventContext.class, multiThreadedTestEventHandler, 3, 30);
    Stage<TestMultiThreadedEventContext> s = multiThreadedStageManager.getStage("testStage2", TestMultiThreadedEventContext.class);
    assertTrue(s != null);
    s.getSink().addMultiThreaded(new TestMultiThreadedEventContext());
    s.getSink().addMultiThreaded(new TestMultiThreadedEventContext());
    s.getSink().addMultiThreaded(new TestMultiThreadedEventContext());
    s.getSink().addMultiThreaded(new TestMultiThreadedEventContext());
    assertTrue(s.getSink().size() == 4);
    assertTrue(multiThreadedTestEventHandler.getContexts().size() == 0);

    s.getSink().addMultiThreaded(new TestMultiThreadedEventContext());
    s.getSink().addMultiThreaded(new TestMultiThreadedEventContext());
    s.getSink().addMultiThreaded(new TestMultiThreadedEventContext());
    s.getSink().addMultiThreaded(new TestMultiThreadedEventContext());
    assertTrue(s.getSink().size() == 8);
    assertTrue(multiThreadedTestEventHandler.getContexts().size() == 0);

    s.start(new ConfigurationContextImpl(null));
    multiThreadedTestEventHandler.waitForEventContextCount(8, 60, SECONDS);
    assertTrue(s.getSink().size() == 0);
    assertTrue(multiThreadedTestEventHandler.getContexts().size() == 8);
    multiThreadedStageManager.stopAll();
  }

  public void testMultiThreadedContextExtended() throws Exception {
    multiThreadedStageManager.createStage("testStage2", TestMultiThreadedEventContext.class, multiThreadedTestEventHandler, 3, 10);
    Stage<TestMultiThreadedEventContext> s = multiThreadedStageManager.getStage("testStage2", TestMultiThreadedEventContext.class);
    assertTrue(s != null);
    s.getSink().addMultiThreaded(new TestMultiThreadedEventContext("Thread-1"));
    s.getSink().addMultiThreaded(new TestMultiThreadedEventContext("Thread-2"));
    s.getSink().addMultiThreaded(new TestMultiThreadedEventContext("Thread-3"));

    s.getSink().addMultiThreaded(new TestMultiThreadedEventContext("Thread-1"));
    assertTrue(s.getSink().size() == 4);
    assertTrue(multiThreadedTestEventHandler.getContexts().size() == 0);

    s.getSink().addMultiThreaded(new TestMultiThreadedEventContext("Thread-2"));
    s.getSink().addMultiThreaded(new TestMultiThreadedEventContext("Thread-3"));

    s.getSink().addMultiThreaded(new TestMultiThreadedEventContext("Thread-1"));
    s.getSink().addMultiThreaded(new TestMultiThreadedEventContext("Thread-2"));
    s.getSink().addMultiThreaded(new TestMultiThreadedEventContext("Thread-3"));
    assertTrue(s.getSink().size() == 9);
    assertTrue(multiThreadedTestEventHandler.getContexts().size() == 0);

    s.start(new ConfigurationContextImpl(null));
    multiThreadedTestEventHandler.waitForEventContextCount(9, 60, SECONDS);
    assertTrue(s.getSink().size() == 0);
    assertTrue(multiThreadedTestEventHandler.getContexts().size() == 9);
    stageManager.stopAll();
  }
  
  /*
   * @see TestCase#tearDown()
   */
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  private static class TestEventContext {
    public TestEventContext() {
      //
    }

  }

  private static class TestMultiThreadedEventContext implements MultiThreadedEventContext {
    final Object name;
    final boolean flush;

    public TestMultiThreadedEventContext() {
      name = new Object();
      flush = false;
    }

    public TestMultiThreadedEventContext(String string) {
      name = string;
      flush = false;
    }

    public TestMultiThreadedEventContext(String string, boolean flush) {
      name = string;
      this.flush = flush;
    }
    
    @Override
    public Object getSchedulingKey() {
      return name;
    }

    @Override
    public boolean flush() {
      return flush;
    }
  }
}
