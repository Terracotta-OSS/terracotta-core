/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management;

import java.util.ResourceBundle;

class ManagementResources {

  private final ResourceBundle resources;

  ManagementResources() {
    resources = ResourceBundle.getBundle("management");
  }

  String getPublicMBeanDomain() {
    return resources.getString("domain.public");
  }

  String getInternalMBeanDomain() {
    return resources.getString("domain.internal");
  }

  String getNodeNameSystemProperty() {
    return resources.getString("system-property.node-name");
  }

  String getDsoClientType() {
    return TerracottaManagement.quoteIfNecessary(resources.getString("type.dso-client"));
  }

  String getSessionsType() {
    return TerracottaManagement.quoteIfNecessary(resources.getString("type.sessions"));
  }

  String getTerracottaServerType() {
    return TerracottaManagement.quoteIfNecessary(resources.getString("type.l2"));
  }

  String getTransactionSubsystem() {
    return TerracottaManagement.quoteIfNecessary(resources.getString("subsystem.tx"));
  }

  String getLockingSubsystem() {
    return TerracottaManagement.quoteIfNecessary(resources.getString("subsystem.locking"));
  }

  String getObjectManagementSubsystem() {
    return TerracottaManagement.quoteIfNecessary(resources.getString("subsystem.object-management"));
  }

  String getNoneSubsystem() {
    return TerracottaManagement.quoteIfNecessary(resources.getString("subsystem.none"));
  }

}