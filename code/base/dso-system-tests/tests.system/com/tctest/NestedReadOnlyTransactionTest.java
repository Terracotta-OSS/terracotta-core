/*
 * Created on Aug 24, 2004 TODO To change the template for this generated file go to Window - Preferences - Java - Code
 * Style - Code Templates
 */
package com.tctest;


public class NestedReadOnlyTransactionTest  extends TransparentTestBase implements TestConfigurator {
  private final static int     NODE_COUNT              = 2;
  
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(1).setIntensity(1);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return NestedReadOnlyTransactionTestApp.class;
  }
}
