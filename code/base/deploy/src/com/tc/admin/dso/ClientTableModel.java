/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.common.XObjectTableModel;

public class ClientTableModel extends XObjectTableModel {
  private static final String[] FIELDS  = {
    "Host",
    "Port",
    "ChannelID"
  };

  private static final String[] HEADERS = {
    AdminClient.getContext().getMessage("dso.client.host"),
    AdminClient.getContext().getMessage("dso.client.port"),
    AdminClient.getContext().getMessage("dso.client.channelID")
  };

  public ClientTableModel() {
    super(DSOClient.class, FIELDS, HEADERS);
  }
}
