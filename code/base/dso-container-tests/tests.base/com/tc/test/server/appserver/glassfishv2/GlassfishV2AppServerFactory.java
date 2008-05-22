/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.glassfishv2;

import com.tc.test.server.appserver.AppServer;
import com.tc.test.server.appserver.AppServerInstallation;
import com.tc.test.server.appserver.glassfish.AbstractGlassfishAppServerFactory;
import com.tc.test.server.appserver.glassfish.GlassfishAppServerInstallation;

public class GlassfishV2AppServerFactory extends AbstractGlassfishAppServerFactory {

  public GlassfishV2AppServerFactory(ProtectedKey protectedKey) {
    super(protectedKey);
  }

  public AppServer createAppServer(AppServerInstallation installation) {
    return new GlassfishV2AppServer((GlassfishAppServerInstallation) installation);
  }


}
