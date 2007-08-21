/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.weblogic9x;

import org.apache.commons.io.IOUtils;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.State;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.weblogic.WebLogic9xInstalledLocalContainer;

import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.cargo.CargoAppServer;
import com.tc.util.ReplaceLine;
import com.tc.util.runtime.Os;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
        setBeaHomeIfNeeded();
        prepareSecurityFile();
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

    private void setBeaHomeIfNeeded() {
      File license = new File(getHome(), "license.bea");
      if (license.exists()) {
        this.setBeaHome(this.getHome());
      }
    }

    private void prepareSecurityFile() {
      if (Os.isLinux()) {
        try {
          String[] resources = new String[] { "user_staged_config/readme.txt",
              "servers/AdminServer/security/boot.properties", "security/XACMLRoleMapperInit.ldift",
              "security/SerializedSystemIni.dat", "security/DefaultRoleMapperInit.ldift",
              "security/DefaultAuthenticatorInit.ldift", "lib/readme.txt", "init-info/tokenValue.properties",
              "init-info/startscript.xml", "init-info/security.xml", "init-info/domain-info.xml",
              "console-ext/readme.txt", "config/config.xml", "config/startup/readme.txt", "config/security/readme.txt",
              "config/nodemanager/nm_password.properties", "config/lib/readme.txt", "config/jms/readme.txt",
              "config/jdbc/readme.txt", "config/diagnostics/readme.txt", "config/deployments/readme.txt",
              "autodeploy/readme.txt", "fileRealm.properties" };
          for (int i = 0; i < resources.length; i++) {
            String resource = "linux/" + resources[i];
            File dest = new File(getConfiguration().getHome(), resources[i]);
            copyResource(resource, dest);
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

      }
    }

    private void copyResource(String name, File dest) throws IOException {
      InputStream in = getClass().getResourceAsStream(name);
      FileOutputStream out = new FileOutputStream(dest);
      try {
        IOUtils.copy(in, out);
      } finally {
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);
      }
    }
  }

}