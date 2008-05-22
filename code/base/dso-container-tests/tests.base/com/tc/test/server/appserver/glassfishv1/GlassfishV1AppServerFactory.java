/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.glassfishv1;

import com.tc.test.server.appserver.AppServer;
import com.tc.test.server.appserver.AppServerInstallation;
import com.tc.test.server.appserver.glassfish.AbstractGlassfishAppServerFactory;
import com.tc.test.server.appserver.glassfish.GlassfishAppServerInstallation;

/**
 * This class creates specific implementations of return values for the given methods. To obtain an instance you must
 * call {@link NewAppServerFactory.createFactoryFromProperties()}.
 */
public final class GlassfishV1AppServerFactory extends AbstractGlassfishAppServerFactory {

  public GlassfishV1AppServerFactory(ProtectedKey protectedKey) {
    super(protectedKey);
  }

  public AppServer createAppServer(AppServerInstallation installation) {
    return new GlassfishV1AppServer((GlassfishAppServerInstallation) installation);
  }

}
