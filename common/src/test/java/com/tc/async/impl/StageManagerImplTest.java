/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.async.impl;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.tc.async.api.EventContext;
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

  private StageManagerImpl      stageManager;
  private TestEventHandler      testEventHandler;

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
      stageManager = new StageManagerImpl(new TCThreadGroup(
                                                            new ThrowableHandlerImpl(TCLogging
                                                                .getLogger(StageManagerImpl.class))),
                                          new QueueFactory());
      testEventHandler = new TestEventHandler();
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  public void testStage() throws Exception {
    stageManager.createStage("testStage", testEventHandler, 1, 3);
    Stage s = stageManager.getStage("testStage");
    assertTrue(s != null);
    s.getSink().add(new TestEventContext());
    assertTrue(s.getSink().size() == 1);
    assertTrue(testEventHandler.getContexts().size() == 0);
    s.getSink().add(new TestEventContext());
    assertTrue(s.getSink().size() == 2);
    assertTrue(testEventHandler.getContexts().size() == 0);
    s.start(new ConfigurationContextImpl(null));
    testEventHandler.waitForEventContextCount(2, 60, SECONDS);
    assertTrue(s.getSink().size() == 0);
    assertTrue(testEventHandler.getContexts().size() == 2);
    stageManager.stopAll();
  }

  public void testMultiThreadedStage() throws Exception {
    stageManager.createStage("testStage2", testEventHandler, 3, 1, 30);
    Stage s = stageManager.getStage("testStage2");
    assertTrue(s != null);
    s.getSink().add(new TestEventContext());
    s.getSink().add(new TestEventContext());
    s.getSink().add(new TestEventContext());
    s.getSink().add(new TestEventContext());
    assertTrue(s.getSink().size() == 4);
    assertTrue(testEventHandler.getContexts().size() == 0);

    s.getSink().add(new TestEventContext());
    s.getSink().add(new TestEventContext());
    s.getSink().add(new TestEventContext());
    s.getSink().add(new TestEventContext());
    assertTrue(s.getSink().size() == 8);
    assertTrue(testEventHandler.getContexts().size() == 0);

    s.start(new ConfigurationContextImpl(null));
    testEventHandler.waitForEventContextCount(8, 60, SECONDS);
    assertTrue(s.getSink().size() == 0);
    assertTrue(testEventHandler.getContexts().size() == 8);
    stageManager.stopAll();
  }

  public void testMultiThreadedContext() throws Exception {
    stageManager.createStage("testStage2", testEventHandler, 3, 1, 30);
    Stage s = stageManager.getStage("testStage2");
    assertTrue(s != null);
    s.getSink().add(new TestMultiThreadedEventContext());
    s.getSink().add(new TestMultiThreadedEventContext());
    s.getSink().add(new TestMultiThreadedEventContext());
    s.getSink().add(new TestMultiThreadedEventContext());
    assertTrue(s.getSink().size() == 4);
    assertTrue(testEventHandler.getContexts().size() == 0);

    s.getSink().add(new TestMultiThreadedEventContext());
    s.getSink().add(new TestMultiThreadedEventContext());
    s.getSink().add(new TestMultiThreadedEventContext());
    s.getSink().add(new TestMultiThreadedEventContext());
    assertTrue(s.getSink().size() == 8);
    assertTrue(testEventHandler.getContexts().size() == 0);

    s.start(new ConfigurationContextImpl(null));
    testEventHandler.waitForEventContextCount(8, 60, SECONDS);
    assertTrue(s.getSink().size() == 0);
    assertTrue(testEventHandler.getContexts().size() == 8);
    stageManager.stopAll();
  }

  public void testMultiThreadedContextExtended() throws Exception {
    stageManager.createStage("testStage2", testEventHandler, 3, 1, 10);
    Stage s = stageManager.getStage("testStage2");
    assertTrue(s != null);
    s.getSink().add(new TestMultiThreadedEventContext("Thread-1"));
    s.getSink().add(new TestMultiThreadedEventContext("Thread-2"));
    s.getSink().add(new TestMultiThreadedEventContext("Thread-3"));

    s.getSink().add(new TestMultiThreadedEventContext("Thread-1"));
    assertTrue(s.getSink().size() == 4);
    assertTrue(testEventHandler.getContexts().size() == 0);

    s.getSink().add(new TestMultiThreadedEventContext("Thread-2"));
    s.getSink().add(new TestMultiThreadedEventContext("Thread-3"));

    s.getSink().add(new TestMultiThreadedEventContext("Thread-1"));
    s.getSink().add(new TestMultiThreadedEventContext("Thread-2"));
    s.getSink().add(new TestMultiThreadedEventContext("Thread-3"));
    assertTrue(s.getSink().size() == 9);
    assertTrue(testEventHandler.getContexts().size() == 0);

    s.start(new ConfigurationContextImpl(null));
    testEventHandler.waitForEventContextCount(9, 60, SECONDS);
    assertTrue(s.getSink().size() == 0);
    assertTrue(testEventHandler.getContexts().size() == 9);
    stageManager.stopAll();
  }

  /*
   * @see TestCase#tearDown()
   */
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  private static class TestEventContext implements EventContext {
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
    public Object getKey() {
      return name;
    }

  }
}
