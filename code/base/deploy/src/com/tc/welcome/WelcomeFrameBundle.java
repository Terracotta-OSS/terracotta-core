/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.welcome;

import java.util.ListResourceBundle;

public class WelcomeFrameBundle extends ListResourceBundle {
  public Object[][] getContents() {
    return new Object[][] { { "welcome.title", "Terracotta Welcome" }, };
  }
}
