/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.async.impl;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.async.api.Stage;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandlerImpl;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.concurrent.QueueFactory;

import junit.framework.TestCase;

/**
 * @author steve
 */
public class StageManagerImplTest extends TestCase {
  private static final TCLogger logging = TCLogging.getLogger(StageManagerImplTest.class);
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
    try {
      stageManager = new StageManagerImpl(new TCThreadGroup(new ThrowableHandlerImpl(TCLogging.getLogger(StageManagerImpl.class))), new QueueFactory<TestEventContext>());
      testEventHandler = new TestEventHandler<>();
      multiThreadedStageManager = new StageManagerImpl(new TCThreadGroup(new ThrowableHandlerImpl(TCLogging.getLogger(StageManagerImpl.class))), new QueueFactory<TestMultiThreadedEventContext>());
      multiThreadedTestEventHandler = new TestEventHandler<>();
    } catch (Throwable t) {
      t.printStackTrace();
    }
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
    stageManager.createStage("testStage2", TestEventContext.class, testEventHandler, 3, 1, 30);
    Stage<TestEventContext> s = stageManager.getStage("testStage2", TestEventContext.class);
    assertTrue(s != null);
    s.getSink().addSingleThreaded(new TestEventContext());
    s.getSink().addSingleThreaded(new TestEventContext());
    s.getSink().addSingleThreaded(new TestEventContext());
    s.getSink().addSingleThreaded(new TestEventContext());
    assertTrue(s.getSink().size() == 4);
    assertTrue(testEventHandler.getContexts().size() == 0);

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
    multiThreadedStageManager.createStage("testStage2", TestMultiThreadedEventContext.class, multiThreadedTestEventHandler, 3, 1, 30);
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
    multiThreadedStageManager.createStage("testStage2", TestMultiThreadedEventContext.class, multiThreadedTestEventHandler, 3, 1, 10);
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

    public TestMultiThreadedEventContext() {
      name = new Object();
    }

    public TestMultiThreadedEventContext(String string) {
      name = string;
    }

    @Override
    public Object getSchedulingKey() {
      return name;
    }

  }
}
