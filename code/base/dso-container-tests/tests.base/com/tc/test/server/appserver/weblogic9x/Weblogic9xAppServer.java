/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.weblogic9x;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.taskdefs.Java;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.State;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.weblogic.WebLogic9xInstalledLocalContainer;

import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.weblogic.WeblogicAppServerBase;
import com.tc.util.runtime.Os;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Weblogic9x AppServer implementation
 */
public final class Weblogic9xAppServer extends WeblogicAppServerBase {

  public Weblogic9xAppServer(Weblogic9xAppServerInstallation installation) {
    super(installation);
  }

  @Override
  protected String cargoServerKey() {
    return "weblogic9x";
  }

  @Override
  protected InstalledLocalContainer container(LocalConfiguration config, AppServerParameters params) {
    return new TCWebLogic9xInstalledLocalContainer(config);
  }

  @Override
  protected void setExtraClasspath(AppServerParameters params) {
    container().setExtraClasspath(params.classpath().split(String.valueOf(File.pathSeparatorChar)));
  }

  @Override
  protected void setConfigProperties(LocalConfiguration config) throws Exception {
    // config.setProperty(WebLogicPropertySet.DOMAIN, "domain");
  }

  private static class TCWebLogic9xInstalledLocalContainer extends WebLogic9xInstalledLocalContainer {

    public TCWebLogic9xInstalledLocalContainer(LocalConfiguration configuration) {
      super(configuration);
    }

    @Override
    public void doStop(Java java) throws Exception {
      WeblogicAppServerBase.doStop(getConfiguration());
    }

    @Override
    protected void setState(State state) {
      if (state.equals(State.STARTING)) {
        setBeaHomeIfNeeded();
        prepareSecurityFile();
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
          for (String resource : resources) {
            File dest = new File(getConfiguration().getHome(), "linux/" + resource);
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
  }

}