/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ApplicationContext;


public interface IAdminClientContext extends ApplicationContext {
  void setAdminClientController(AdminClientController controller);
  AdminClientController getAdminClientController();
  AbstractNodeFactory getNodeFactory();
}
