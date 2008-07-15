/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.jboss_common;

import org.apache.commons.io.FileUtils;

import com.tc.util.PortChooser;
import com.tc.util.ReplaceLine;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

public class JBossHelper {
  public static void startupActions(File serverDir, Collection sars) throws IOException {
    writePortsConfig(new PortChooser(), new File(serverDir, "conf/cargo-binding.xml"));

    for (Iterator i = sars.iterator(); i.hasNext();) {
      File sarFile = (File) i.next();
      File deploy = new File(serverDir, "deploy");
      FileUtils.copyFileToDirectory(sarFile, deploy);
    }
  }

  private static void writePortsConfig(PortChooser pc, File dest) throws IOException {
    ReplaceLine.Token[] tokens = new ReplaceLine.Token[13];
    int rmiPort = pc.chooseRandomPort();
    int rmiObjPort = new PortChooser().chooseRandomPort();

    tokens[0] = new ReplaceLine.Token(14, "(RmiPort\">[0-9]+)", "RmiPort\">" + rmiPort);
    tokens[1] = new ReplaceLine.Token(50, "(port=\"[0-9]+)", "port=\"" + rmiPort);
    tokens[2] = new ReplaceLine.Token(24, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort());
    tokens[3] = new ReplaceLine.Token(32, "(port=\"[0-9]+)", "port=\"" + rmiObjPort);
    tokens[4] = new ReplaceLine.Token(64, "(port=\"[0-9]+)", "port=\"" + rmiObjPort);
    tokens[5] = new ReplaceLine.Token(40, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort());
    tokens[6] = new ReplaceLine.Token(94, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort());
    tokens[7] = new ReplaceLine.Token(101, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort());
    tokens[8] = new ReplaceLine.Token(112, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort());
    tokens[9] = new ReplaceLine.Token(57, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort());
    tokens[10] = new ReplaceLine.Token(74, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort());
    tokens[11] = new ReplaceLine.Token(177, "(select=\"[^\"]+\")", "select=\"" + pc.chooseRandomPort() + "\"");
    tokens[12] = new ReplaceLine.Token(178, "(select=\"[^\"]+\")", "select=\"" + pc.chooseRandomPort() + "\"");

    ReplaceLine.parseFile(tokens, dest);
  }

}
