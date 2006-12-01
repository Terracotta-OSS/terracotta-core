/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.welcome;

import java.util.ListResourceBundle;

public class DSOSamplesFrameBundle extends ListResourceBundle {
  public Object[][] getContents() {
    return contents;
  }

  static final Object[][] contents = {
    {"frame.title", "Sample Application Launcher"},
    {"jvm.coordination", "JVM Coordination"},
    {"shared.work.queue", "Shared Work Queue"},
    {"starting.jtable", "Staring Shared JTable..."},
    {"starting.shared.editor", "Starting Shared Graphics Editor..."},
    {"starting.chatter", "Starting Chatter..."},
    {"starting.jvm.coordination", "Starting JVM Coordination..."},
    {"starting.shared.queue", "Starting Shared Work Queue..."}
  };
}
