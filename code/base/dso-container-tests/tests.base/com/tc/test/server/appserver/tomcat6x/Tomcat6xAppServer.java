/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.tomcat6x;

import org.apache.commons.io.FileUtils;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.State;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.tomcat.Tomcat6xInstalledLocalContainer;

import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.ValveDefinition;
import com.tc.test.server.appserver.cargo.CargoAppServer;
import com.tc.test.server.util.AppServerUtil;
import com.tc.util.ReplaceLine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Tomcat6x AppServer implementation
 */
public final class Tomcat6xAppServer extends CargoAppServer {

  public Tomcat6xAppServer(Tomcat6xAppServerInstallation installation) {
    super(installation);
  }

  @Override
  protected String cargoServerKey() {
    return "tomcat6x";
  }

  @Override
  protected InstalledLocalContainer container(LocalConfiguration config, AppServerParameters params) {
    return new TCTomcat6xInstalledLocalContainer(config, params);
  }

  @Override
  protected void setConfigProperties(LocalConfiguration config) throws Exception {
    config.setProperty(GeneralPropertySet.RMI_PORT, Integer.toString(AppServerUtil.getPort()));
  }

  private static class TCTomcat6xInstalledLocalContainer extends Tomcat6xInstalledLocalContainer {

    private final AppServerParameters params;

    public TCTomcat6xInstalledLocalContainer(LocalConfiguration config, AppServerParameters params) {
      super(config);
      this.params = params;
    }

    @Override
    protected void setState(State state) {
      if (state.isStarting()) {
        try {
          // add Vavles (if defined)
          Collection<ValveDefinition> valves = params.valves();
          if (!valves.isEmpty()) {
            String valvesXml = "";
            for (ValveDefinition def : valves) {
              valvesXml += "\n" + def.toXml();
            }

            List<ReplaceLine.Token> tokens = new ArrayList<ReplaceLine.Token>();
            File serverXml = new File(getConfiguration().getHome(), "conf/server.xml");
            tokens.add(new ReplaceLine.Token(28, "</Context>", valvesXml + "\n</Context>"));
            ReplaceLine.parseFile(tokens.toArray(new ReplaceLine.Token[] {}), serverXml);
          }

          // add in custom server jars
          Collection<String> tomcatServerJars = params.tomcatServerJars();
          if (!tomcatServerJars.isEmpty()) {
            String jarsCsv = "";
            String[] jars = tomcatServerJars.toArray(new String[] {});
            for (int i = 0; i < jars.length; i++) {
              jarsCsv += jars[i].replace('\\', '/');
              if (i < jars.length - 1) {
                jarsCsv += ",";
              }
            }

            File catalinaProps = new File(getConfiguration().getHome(), "conf/catalina.properties");
            FileUtils.copyFile(new File(getHome(), "conf/catalina.properties"), catalinaProps);

            List<ReplaceLine.Token> tokens = new ArrayList<ReplaceLine.Token>();
            tokens.add(new ReplaceLine.Token(47, ".jar", ".jar," + jarsCsv));
            ReplaceLine.parseFile(tokens.toArray(new ReplaceLine.Token[] {}), catalinaProps);
          }
        } catch (IOException ioe) {
          throw new RuntimeException(ioe);
        }
      }

      super.setState(state);
    }
  }

}
