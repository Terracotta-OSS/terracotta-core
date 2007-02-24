package com.tctest;

/*
 * Unit test for measuring the overhead of the instrumented Field class. For correctness
 * tests for instrumented Field class, refer to the ReflectionFieldTest.
 */
public class IBatisSimpleDaoTest extends TransparentTestBase {
  private final static int NODE_COUNT = 2;

  private final static int LOOP_COUNT = 1;
  
  public IBatisSimpleDaoTest() {
    // disableAllUntil("2007-02-28");
  }

  public void setUp() throws Exception {
    super.setUp();

    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(LOOP_COUNT);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return IBatisSimpleDaoTestApp.class;
  }

  public void testPluginsLoaded() {
    assertTrue(true);
  }

}
