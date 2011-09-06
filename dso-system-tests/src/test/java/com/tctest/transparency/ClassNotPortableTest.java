/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.transparency;

import com.tctest.TestConfigurator;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;


public class ClassNotPortableTest extends TransparentTestBase implements TestConfigurator {

  protected Class getApplicationClass() {
    return ClassNotPortableTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(1).setIntensity(1);
    t.initializeTestRunner();
  }
  
}
