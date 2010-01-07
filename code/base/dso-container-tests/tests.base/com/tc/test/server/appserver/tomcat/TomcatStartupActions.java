/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.test.server.appserver.tomcat;

import org.apache.commons.io.FileUtils;
import org.codehaus.cargo.container.InstalledLocalContainer;

import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.ValveDefinition;
import com.tc.util.ReplaceLine;
import com.tc.util.runtime.Os;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TomcatStartupActions {
  private TomcatStartupActions() {
    //
  }

  public static void modifyConfig(AppServerParameters params, InstalledLocalContainer container, int catalinaPropsLine) {
    try {
      // add Vavles (if defined)
      Collection<ValveDefinition> valves = params.valves();
      if (!valves.isEmpty()) {
        String valvesXml = "";
        for (ValveDefinition def : valves) {
          valvesXml += "\n" + def.toXml();
        }

        List<ReplaceLine.Token> tokens = new ArrayList<ReplaceLine.Token>();
        File serverXml = new File(container.getConfiguration().getHome(), "conf/server.xml");
        tokens.add(new ReplaceLine.Token(28, "</Context>", valvesXml + "\n</Context>"));
        ReplaceLine.parseFile(tokens.toArray(new ReplaceLine.Token[] {}), serverXml);
      }

      // add in custom server jars
      Collection<String> tomcatServerJars = params.tomcatServerJars();
      if (!tomcatServerJars.isEmpty()) {
        String jarsCsv = "";
        String[] jars = tomcatServerJars.toArray(new String[] {});
        for (int i = 0; i < jars.length; i++) {
          jarsCsv += "file:" + (Os.isWindows() ? "/" : "") + jars[i].replace('\\', '/');
          if (i < jars.length - 1) {
            jarsCsv += ",";
          }
        }

        File catalinaProps = new File(container.getConfiguration().getHome(), "conf/catalina.properties");
        FileUtils.copyFile(new File(container.getHome(), "conf/catalina.properties"), catalinaProps);

        List<ReplaceLine.Token> tokens = new ArrayList<ReplaceLine.Token>();
        tokens.add(new ReplaceLine.Token(catalinaPropsLine, ".jar$", ".jar," + jarsCsv));
        ReplaceLine.parseFile(tokens.toArray(new ReplaceLine.Token[] {}), catalinaProps);
      }
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

}
