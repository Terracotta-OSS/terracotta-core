/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

import com.tc.admin.common.AbstractApplication;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.LAFHelper;

public class SessionIntegrator extends AbstractApplication {
  private SessionIntegratorContext context;

  private SessionIntegrator() {
    super();
    context = new SessionIntegratorContext(this);
  }

  public ApplicationContext getApplicationContext() {
    return context;
  }

  public void start() {
    SessionIntegratorFrame frame = new SessionIntegratorFrame(context);
    frame.setVisible(true);
  }

  public static final void main(String[] args) throws Exception {
    if (System.getProperty("swing.defaultlaf") == null) {
      args = LAFHelper.parseLAFArgs(args);
    }
    SessionIntegrator client = new SessionIntegrator();
    client.parseArgs(args);
    client.start();
  }
}
