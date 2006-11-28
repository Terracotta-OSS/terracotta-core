package com.tc.welcome;

import java.util.ListResourceBundle;

public class DSOSamplesFrameBundle extends ListResourceBundle {
  public Object[][] getContents() {
    return contents;
  }

  static final Object[][] contents = {
    {"frame.title", "Sample Application Launcher"},
    {"jvm.coordinate", "JVM Coordination"},
    {"shared.work.queue", "Shared Work Queue"},
    {"starting.jtable", "Staring Shared JTable..."},
    {"starting.shared.editor", "Starting Shared Graphics Editor..."},
    {"starting.chatter", "Starting Chatter..."},
    {"starting.jvm.coordination", "Starting JVM Coordination..."},
    {"starting.shared.queue", "Starting Shared Work Queue..."}
  };
}
