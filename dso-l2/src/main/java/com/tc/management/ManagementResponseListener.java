/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.object.msg.AbstractManagementMessage;

/**
 *
 */
public interface ManagementResponseListener {

  void onResponse(AbstractManagementMessage message);

}
