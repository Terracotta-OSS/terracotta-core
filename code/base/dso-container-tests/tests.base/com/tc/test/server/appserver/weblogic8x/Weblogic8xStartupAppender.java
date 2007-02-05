/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.weblogic8x;

import com.tc.test.server.appserver.cargo.CargoStartupAppender;
import com.tc.util.ReplaceLine;

import java.io.File;

public class Weblogic8xStartupAppender extends CargoStartupAppender {

  public void append() throws Exception {
    ReplaceLine.Token[] tokens = new ReplaceLine.Token[1];
    tokens[0] = new ReplaceLine.Token(
                                      5,
                                      "(NativeIOEnabled=\"false\")",
                                      "NativeIOEnabled=\"false\" SocketReaderTimeoutMaxMillis=\"1000\" SocketReaderTimeoutMinMillis=\"1000\" StdoutDebugEnabled=\"true\" StdoutSeverityLevel=\"64\"");
    ReplaceLine.parseFile(tokens, new File("config.xml"));
  }
}
