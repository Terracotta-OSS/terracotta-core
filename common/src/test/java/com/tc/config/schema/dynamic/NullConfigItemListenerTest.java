/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

import com.tc.test.TCTestCase;

/**
 * Unit test for {@link NullConfigItemListener}.
 */
public class NullConfigItemListenerTest extends TCTestCase {

  public void testAll() throws Exception {
    NullConfigItemListener.getInstance().valueChanged(null, null);
    NullConfigItemListener.getInstance().valueChanged("foo", null);
    NullConfigItemListener.getInstance().valueChanged(null, "bar");
    NullConfigItemListener.getInstance().valueChanged("foo", "bar");
  }

}
