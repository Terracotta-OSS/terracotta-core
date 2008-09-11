/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.weblogic10x;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.types.Path;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.State;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.weblogic.WebLogic10xInstalledLocalContainer;

import com.tc.test.AppServerInfo;
import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.cargo.CargoAppServer;
import com.tc.util.ReplaceLine;
import com.tc.util.runtime.Os;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Weblogic10x AppServer implementation
 */
public final class Weblogic10xAppServer extends CargoAppServer {

  public Weblogic10xAppServer(Weblogic10xAppServerInstallation installation) {
    super(installation);
  }

  protected String cargoServerKey() {
    return "weblogic10x";
  }

  protected InstalledLocalContainer container(LocalConfiguration config, AppServerParameters params) {
    return new TCWebLogic10xInstalledLocalContainer(config);
  }

  protected void setExtraClasspath(AppServerParameters params) {
    container().setExtraClasspath(params.classpath().split(String.valueOf(File.pathSeparatorChar)));
  }

  protected void setConfigProperties(LocalConfiguration config) throws Exception {
    // config.setProperty(WebLogicPropertySet.DOMAIN, "domain");
  }

  private static class TCWebLogic10xInstalledLocalContainer extends WebLogic10xInstalledLocalContainer {

    public TCWebLogic10xInstalledLocalContainer(LocalConfiguration configuration) {
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
      if (Os.isLinux() || Os.isSolaris()) {
        try {
          String[] resources = new String[] { "security/SerializedSystemIni.dat" };
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
      dest.getParentFile().mkdirs();
      InputStream in = getClass().getResourceAsStream(name);
      FileOutputStream out = new FileOutputStream(dest);
      try {
        IOUtils.copy(in, out);
      } finally {
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);
      }
    }

    protected void addToClassPath(Path classpath) {
      AppServerInfo appServerInfo = TestConfigObject.getInstance().appServerInfo();
      File modulesDir = new File(this.getHome(), "modules");
      if (appServerInfo.toString().equals("weblogic-10.0.mp1")) {
        classpath.createPathElement()
            .setLocation(new File(modulesDir, "features/weblogic.server.modules_10.0.1.0.jar"));
        classpath.createPathElement().setLocation(
                                                  new File(modulesDir,
                                                           "features/com.bea.cie.common-plugin.launch_2.1.2.0.jar"));
        classpath.createPathElement().setLocation(new File(modulesDir, "org.apache.ant_1.6.5/lib/ant-all.jar"));
        classpath.createPathElement()
            .setLocation(new File(modulesDir, "net.sf.antcontrib_1.0b2.0/lib/ant-contrib.jar"));
      } else if (appServerInfo.toString().equals("weblogic-10.3.0")) {
        classpath.createPathElement()
            .setLocation(new File(modulesDir, "features/weblogic.server.modules_10.3.0.0.jar"));
        classpath.createPathElement().setLocation(new File(modulesDir, "org.apache.ant_1.6.5/lib/ant-all.jar"));
        classpath.createPathElement().setLocation(
                                                  new File(modulesDir,
                                                           "net.sf.antcontrib_1.0.0.0_1-0b2/lib/ant-contrib.jar"));
      }
    }
  }

}