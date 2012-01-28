/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.welcome;

import java.util.ListResourceBundle;

public class DSOSamplesFrameBundle extends ListResourceBundle {
  public Object[][] getContents() {
    return new Object[][] {
        { "frame.title", "Sample Application Launcher" },
        { "servers.action.name", "Servers" },
        { "servers.use.local", "Use local server" },
        { "servers.use.remote", "Use remote servers" },
        { "servers.field.tip", "Example: server1:9510,server2:9510" },
        { "servers.field.description",
            "<html>A comma-separated list of server specifications:<br><p align=center>example: server1:9510,server2:9510</html>" },
        { "jvm.coordination", "JVM Coordination" }, { "shared.work.queue", "Shared Work Queue" },
        { "starting.jtable", "Staring Shared JTable..." },
        { "starting.shared.editor", "Starting Shared Graphics Editor..." },
        { "starting.chatter", "Starting Chatter..." }, { "starting.jvm.coordination", "Starting JVM Coordination..." },
        { "starting.shared.queue", "Starting Shared Work Queue..." } };
  }
}
