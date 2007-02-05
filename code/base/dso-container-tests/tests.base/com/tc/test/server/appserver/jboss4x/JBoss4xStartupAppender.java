/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.jboss4x;

import com.tc.test.server.appserver.cargo.CargoStartupAppender;
import com.tc.util.PortChooser;
import com.tc.util.ReplaceLine;

import java.io.File;

public class JBoss4xStartupAppender extends CargoStartupAppender {

  public void append() throws Exception {
    ReplaceLine.Token[] tokens = new ReplaceLine.Token[6];
    int rmiPort = new PortChooser().chooseRandomPort();
    tokens[0] = new ReplaceLine.Token(14, "(RmiPort\">[0-9]+)", "RmiPort\">" + rmiPort);
    tokens[1] = new ReplaceLine.Token(50, "(port=\"[0-9]+)", "port=\"" + rmiPort);
    tokens[2] = new ReplaceLine.Token(24, "(port=\"[0-9]+)", "port=\"" + new PortChooser().chooseRandomPort());
    int rmiObjPort = new PortChooser().chooseRandomPort();
    tokens[3] = new ReplaceLine.Token(32, "(port=\"[0-9]+)", "port=\"" + rmiObjPort);
    tokens[4] = new ReplaceLine.Token(64, "(port=\"[0-9]+)", "port=\"" + rmiObjPort);
    tokens[5] = new ReplaceLine.Token(40, "(port=\"[0-9]+)", "port=\"" + new PortChooser().chooseRandomPort());
    ReplaceLine.parseFile(tokens, new File("conf/cargo-binding.xml"));
  }
}
