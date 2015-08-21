/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.api;

import com.tc.net.groups.ActiveServerIDManager;
import com.tc.net.groups.GroupManager;

public interface EnterpriseServerConfigurationContext extends ServerConfigurationContext {

  public static final String AA_TRANSACTION_WATERMARK_BROADCAST_STAGE = "aa_transaction_watermark_broadcast_stage";
  public static final String AA_TRANSACTION_WATERMARK_RECEIVE_STAGE   = "aa_transaction_watermark_receive_stage";

  public ActiveServerIDManager getActiveServerIDManager();

  public GroupManager getActiveServerGroupManager();

}
