/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.AbstractApplication;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.LAFHelper;

public class AdminClient extends AbstractApplication {
  protected IAdminClientContext context;

  public AdminClient() {
    super();
    context = new AdminClientContext(this);
  }

  public ApplicationContext getApplicationContext() {
    return context;
  }

  @Override
  public void start() {
    AdminClientFrame frame = new AdminClientFrame(context);
    frame.setVisible(true);
  }

  public static void main(String[] args) throws Exception {
    if (System.getProperty("swing.defaultlaf") == null) {
      args = LAFHelper.parseLAFArgs(args);
    }
    AdminClient client = new AdminClient();
    client.parseArgs(args);
    client.start();
  }
}
