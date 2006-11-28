package com.tc.welcome;

import java.util.ListResourceBundle;

public class WelcomeFrameBundle extends ListResourceBundle {
  public Object[][] getContents() {
    return contents;
  }

  static final Object[][] contents = {
    {"welcome.title", "Terracotta Welcome"},
    {"dso.welcome.title", "Terracotta DSO Welcome"},
    {"sessions.welcome.title", "Terracotta Sessions Welcome"},
    {"spring.welcome.title", "Terracotta for Spring Welcome"}
  };
}
