/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.weblogic8x;

import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.State;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.weblogic.WebLogic8xInstalledLocalContainer;

import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.cargo.CargoAppServer;
import com.tc.util.ReplaceLine;

import java.io.File;
import java.io.IOException;

/**
 * Weblogic8x AppServer implementation
 */
public final class Weblogic8xAppServer extends CargoAppServer {

  public Weblogic8xAppServer(Weblogic8xAppServerInstallation installation) {
    super(installation);
  }

  protected String cargoServerKey() {
    return "weblogic8x";
  }

  protected InstalledLocalContainer container(LocalConfiguration config) {
    return new TCWebLogic8xInstalledLocalContainer(config);
  }

  protected void setExtraClasspath(AppServerParameters params) {
    container().setExtraClasspath(params.classpath().split(String.valueOf(File.pathSeparatorChar)));
  }

  protected void setConfigProperties(LocalConfiguration config) throws Exception {
    // config.setProperty(WebLogicPropertySet.DOMAIN, "domain");
  }

  private static class TCWebLogic8xInstalledLocalContainer extends WebLogic8xInstalledLocalContainer {

    public TCWebLogic8xInstalledLocalContainer(LocalConfiguration configuration) {
      super(configuration);
    }

    protected void setState(State state) {
      if (state.equals(State.STARTING)) {
        adjustConfig();
        File license = new File(getHome(), "license.bea");
        if (license.exists()) {
          this.setBeaHome(this.getHome());
        }
      }
    }

    private void adjustConfig() {
      ReplaceLine.Token[] tokens = new ReplaceLine.Token[1];
      tokens[0] = new ReplaceLine.Token(
                                        5,
                                        "(NativeIOEnabled=\"false\")",
                                        "NativeIOEnabled=\"false\" SocketReaderTimeoutMaxMillis=\"1000\" SocketReaderTimeoutMinMillis=\"1000\" StdoutDebugEnabled=\"true\" StdoutSeverityLevel=\"64\"");

      try {
        ReplaceLine.parseFile(tokens, new File(getConfiguration().getHome(), "config.xml"));
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }
  }

}