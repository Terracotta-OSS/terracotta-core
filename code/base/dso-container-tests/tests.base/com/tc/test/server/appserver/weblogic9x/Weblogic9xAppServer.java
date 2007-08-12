/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.weblogic9x;

import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.State;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.weblogic.WebLogic9xInstalledLocalContainer;

import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.cargo.CargoAppServer;
import com.tc.util.ReplaceLine;

import java.io.File;
import java.io.IOException;

/**
 * Weblogic9x AppServer implementation
 */
public final class Weblogic9xAppServer extends CargoAppServer {

  public Weblogic9xAppServer(Weblogic9xAppServerInstallation installation) {
    super(installation);
  }

  protected String cargoServerKey() {
    // this is intentional, since cargo doesn't support weblogic92
    // we force it to support weblogic92 by faking weblogic8x
    return "weblogic9x";
  }

  protected InstalledLocalContainer container(LocalConfiguration config) {
    return new TCWebLogic9xInstalledLocalContainer(config);
  }

  protected void setExtraClasspath(AppServerParameters params) {
    container().setExtraClasspath(params.classpath().split(String.valueOf(File.pathSeparatorChar)));
  }

  protected void setConfigProperties(LocalConfiguration config) throws Exception {
    // config.setProperty(WebLogicPropertySet.DOMAIN, "domain");
  }

  private static class TCWebLogic9xInstalledLocalContainer extends WebLogic9xInstalledLocalContainer {

    public TCWebLogic9xInstalledLocalContainer(LocalConfiguration configuration) {
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
        ReplaceLine.parseFile(tokens, new File(getConfiguration().getHome(), "/config/config.xml"));
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }
  }

}