/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.config.schema.listen;

import com.tc.config.schema.MockXmlObject;
import com.tc.test.TCTestCase;

/**
 * Unit test for {@link NullConfigurationChangeListener}.
 */
public class NullConfigurationChangeListenerTest extends TCTestCase {

  public void testAll() throws Exception {
    NullConfigurationChangeListener.getInstance().configurationChanged(null, null);
    NullConfigurationChangeListener.getInstance().configurationChanged(null, new MockXmlObject());
    NullConfigurationChangeListener.getInstance().configurationChanged(new MockXmlObject(), null);
    NullConfigurationChangeListener.getInstance().configurationChanged(new MockXmlObject(), new MockXmlObject());
  }

}
