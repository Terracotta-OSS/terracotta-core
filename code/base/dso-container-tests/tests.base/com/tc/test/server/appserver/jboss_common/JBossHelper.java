/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.jboss_common;

import org.apache.commons.io.FileUtils;

import com.tc.test.AppServerInfo;
import com.tc.util.PortChooser;
import com.tc.util.ReplaceLine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class JBossHelper {
  public static void startupActions(File serverDir, Collection sars, AppServerInfo appServerInfo) throws IOException {
    writePortsConfig(new PortChooser(), new File(serverDir, "conf/cargo-binding.xml"), appServerInfo);

    for (Iterator i = sars.iterator(); i.hasNext();) {
      File sarFile = (File) i.next();
      File deploy = new File(serverDir, "deploy");
      FileUtils.copyFileToDirectory(sarFile, deploy);
    }
  }

  private static void writePortsConfig(PortChooser pc, File dest, AppServerInfo appServerInfo) throws IOException {
    List<ReplaceLine.Token> tokens = new ArrayList<ReplaceLine.Token>();

    int rmiPort = pc.chooseRandomPort();
    int rmiObjPort = new PortChooser().chooseRandomPort();

    tokens.add(new ReplaceLine.Token(14, "(RmiPort\">[0-9]+)", "RmiPort\">" + rmiPort));
    tokens.add(new ReplaceLine.Token(50, "(port=\"[0-9]+)", "port=\"" + rmiPort));
    tokens.add(new ReplaceLine.Token(24, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort()));
    tokens.add(new ReplaceLine.Token(32, "(port=\"[0-9]+)", "port=\"" + rmiObjPort));
    tokens.add(new ReplaceLine.Token(64, "(port=\"[0-9]+)", "port=\"" + rmiObjPort));
    tokens.add(new ReplaceLine.Token(40, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort()));
    tokens.add(new ReplaceLine.Token(94, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort()));
    tokens.add(new ReplaceLine.Token(101, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort()));
    tokens.add(new ReplaceLine.Token(112, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort()));
    tokens.add(new ReplaceLine.Token(57, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort()));
    tokens.add(new ReplaceLine.Token(74, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort()));
    tokens.add(new ReplaceLine.Token(177, "(select=\"[^\"]+\")", "select=\"" + pc.chooseRandomPort() + "\""));
    tokens.add(new ReplaceLine.Token(178, "(select=\"[^\"]+\")", "select=\"" + pc.chooseRandomPort() + "\""));

    // XXX: This isn't great, but it will do for now. Each version of cargo-binding.xml should have it's own definition
    // for this stuff, as opposed to the conditional logic in here
    if (appServerInfo.getMajor().equals("4") && appServerInfo.getMinor().startsWith("2")) {
      tokens.add(new ReplaceLine.Token(39, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort()));
      tokens.add(new ReplaceLine.Token(56, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort()));
      tokens.add(new ReplaceLine.Token(62, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort()));

      int ejb3HandlerPort = pc.chooseRandomPort();
      tokens.add(new ReplaceLine.Token(170, "(:3873)", ":" + ejb3HandlerPort));
      tokens.add(new ReplaceLine.Token(172, "(port=\"[0-9]+)", "port=\"" + ejb3HandlerPort));

      tokens.add(new ReplaceLine.Token(109, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort()));
      tokens.add(new ReplaceLine.Token(264, "(port=\"[0-9]+)", "port=\"" + pc.chooseRandomPort()));
    }

    ReplaceLine.parseFile(tokens.toArray(new ReplaceLine.Token[] {}), dest);
  }
}
