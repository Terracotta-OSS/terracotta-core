/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.EventContext;
import com.tc.async.api.Stage;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.concurrent.ThreadUtil;

import junit.framework.TestCase;

/**
 * @author steve To change the template for this generated type comment go to Window&gt;Preferences&gt;Java&gt;Code
 *         Generation&gt;Code and Comments
 */
public class StageManagerImplTest extends TestCase {
  private static final TCLogger logging = TCLogging.getLogger(StageManagerImplTest.class);
  static {
    logging.info("I have to load this class for breaking circular dependency");
  }
  
  private StageManagerImpl stageManager;
  private TestEventHandler testEventHandler;

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
  protected void setUp() throws Exception {
    super.setUp();
    try {
    stageManager = new StageManagerImpl(new TCThreadGroup(new ThrowableHandler(TCLogging
        .getLogger(StageManagerImpl.class))));
    testEventHandler = new TestEventHandler();
    } catch(Throwable t) {
      t.printStackTrace();
    }
  }

  public void testStage() {
    stageManager.createStage("testStage", testEventHandler, 1, 3);
    Stage s = stageManager.getStage("testStage");
    assertTrue(s != null);
    s.getSink().add(new TestEventContext(1));
    assertTrue(s.getSink().size() == 1);
    assertTrue(testEventHandler.getContexts().size() == 0);
    s.getSink().add(new TestEventContext(2));
    assertTrue(s.getSink().size() == 2);
    assertTrue(testEventHandler.getContexts().size() == 0);
    s.start(new ConfigurationContextImpl(null));
    ThreadUtil.reallySleep(1000);
    System.out.println("size=" + s.getSink().size());
    assertTrue(s.getSink().size() == 0);
    assertTrue(testEventHandler.getContexts().size() == 2);
    stageManager.stopAll();
  }

  /*
   * @see TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  private static class TestEventContext implements EventContext {
    private int id;

    public TestEventContext(int id) {
      this.id = id;
    }

    public int getID() {
      return id;
    }
  }
}
