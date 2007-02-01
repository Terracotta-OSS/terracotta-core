/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.welcome;

import java.util.ListResourceBundle;

public class WelcomeFrameBundle extends ListResourceBundle {
  public Object[][] getContents() {
    return contents;
  }

  static final Object[][] contents = {
    {"welcome.title", "Terracotta Welcome"},
    {"pojos.welcome.title", "Terracotta POJO's Welcome"},
    {"sessions.welcome.title", "Terracotta Sessions Welcome"},
    {"spring.welcome.title", "Terracotta for Spring Welcome"}
  };
}
