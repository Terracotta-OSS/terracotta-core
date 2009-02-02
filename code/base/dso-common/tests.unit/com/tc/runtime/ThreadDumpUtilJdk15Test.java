/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.runtime;

import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.runtime.ThreadDumpUtil;
import com.tc.util.runtime.ThreadDumpUtilJdk15;

public class ThreadDumpUtilJdk15Test extends TCTestCase {

  public void testThreadDump15() {
    String st = ThreadDumpUtilJdk15.getThreadDump();
    Assert.eval("The text \"Full thread dump \" should be present in the thread dump",
                st.indexOf("Full thread dump ") >= 0);
  }

  public void testThreadDump14() {
    String st = ThreadDumpUtil.getThreadDump();
    Assert.eval("The text \"Full thread dump \" should be present in the thread dump",
                st.indexOf("Full thread dump ") >= 0);
  }
}
