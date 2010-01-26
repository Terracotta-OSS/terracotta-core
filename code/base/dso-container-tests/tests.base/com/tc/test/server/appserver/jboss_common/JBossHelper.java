/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.jboss_common;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.tc.test.AppServerInfo;
import com.tc.util.PortChooser;
import com.tc.util.ReplaceLine;
import com.tc.util.runtime.Os;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class JBossHelper {
  public static void startupActions(File serverDir, Collection sars, AppServerInfo appServerInfo,
                                    Collection<String> tomcatServerJars) throws IOException {
    if (appServerInfo.getMajor().equals("5") && appServerInfo.getMinor().startsWith("1")) {
      writePortsConfigJBoss51x(new PortChooser(), serverDir, appServerInfo);
    } else {
      writePortsConfig(new PortChooser(), new File(serverDir, "conf/cargo-binding.xml"), appServerInfo);
    }

    // add server_xxx lib dir to classpath
    String slashes = Os.isWindows() ? "/" : "//";

    int classPathLine = findFirstLine(new File(serverDir, "conf/jboss-service.xml"), "^.*<classpath .*$");
    String serverLib = new File(serverDir, "lib").getAbsolutePath().replace('\\', '/');
    ReplaceLine.Token[] tokens = new ReplaceLine.Token[] { new ReplaceLine.Token(
                                                                                 classPathLine,
                                                                                 "<classpath",
                                                                                 "<classpath codebase=\"file:"
                                                                                     + slashes
                                                                                     + serverLib
                                                                                     + "\" archives=\"*\"/>\n    <classpath") };
    ReplaceLine.parseFile(tokens, new File(serverDir, "conf/jboss-service.xml"));

    File dest = new File(serverLib);
    dest.mkdirs();
    for (String jar : tomcatServerJars) {
      FileUtils.copyFileToDirectory(new File(jar), dest);
    }

    for (Iterator i = sars.iterator(); i.hasNext();) {
      File sarFile = (File) i.next();
      File deploy = new File(serverDir, "deploy");
      FileUtils.copyFileToDirectory(sarFile, deploy);
    }
  }

  private static int findFirstLine(File file, String pattern) throws IOException {
    BufferedReader reader = null;

    try {
      int lineNum = 0;
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

      String line;
      while ((line = reader.readLine()) != null) {
        lineNum++;
        if (line.matches(pattern)) { return lineNum; }
      }
    } finally {
      IOUtils.closeQuietly(reader);
    }

    throw new RuntimeException("pattern [" + pattern + "] not found in " + file);
  }

  private static void writePortsConfigJBoss51x(PortChooser pc, File serverDir, AppServerInfo appServerInfo)
      throws IOException {
    List<ReplaceLine.Token> tokens = new ArrayList<ReplaceLine.Token>();
    File dest = new File(serverDir, "conf/bindingservice.beans/META-INF/bindings-jboss-beans.xml");

    // line 110, 280, 451 contains ports which already handled by Cargo
    int[] lines = new int[] { 117, 124, 131, 158, 165, 174, 181, 189, 212, 219, 227, 236, 243, 251, 306, 315, 322, 332,
        340, 349 };
    for (int line : lines) {
      int port = pc.chooseRandomPort();
      tokens.add(new ReplaceLine.Token(line, "\"port\">[0-9]+", "\"port\">" + port));
    }

    ReplaceLine.parseFile(tokens.toArray(new ReplaceLine.Token[] {}), dest);

    // fix up "caculated" AJP and https ports (since they can collide and drop below 1024)
    tokens.clear();
    for (int line : new int[] { 440, 441 }) {
      int port = pc.chooseRandomPort();
      tokens.add(new ReplaceLine.Token(line, "select=\"\\$port . [0-9]+\"", "select=\"" + port + "\""));
    }

    ReplaceLine.parseFile(tokens.toArray(new ReplaceLine.Token[] {}), dest);

    // handling another file
    tokens.clear();
    dest = new File(serverDir, "deploy/ejb3-connectors-jboss-beans.xml");
    tokens.add(new ReplaceLine.Token(36, "3873", String.valueOf(pc.chooseRandomPort())));
    ReplaceLine.parseFile(tokens.toArray(new ReplaceLine.Token[] {}), dest);
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
