/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management;

import java.util.ResourceBundle;

class ManagementResources {

  private final ResourceBundle resources;

  ManagementResources() {
    resources = ResourceBundle.getBundle(getClass().getPackage().getName() + ".management");
  }

  String getPublicMBeanDomain() {
    return resources.getString("domain.public");
  }

  String getInternalMBeanDomain() {
    return resources.getString("domain.internal");
  }

  String getTimMBeanDomain() {
    return resources.getString("domain.tim");
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

  public String getTerracottaClusterType() {
    return TerracottaManagement.quoteIfNecessary(resources.getString("type.cluster"));
  }

  String getTerracottaServerType() {
    return TerracottaManagement.quoteIfNecessary(resources.getString("type.l2"));
  }

  String getTerracottaAgentType() {
    return TerracottaManagement.quoteIfNecessary(resources.getString("type.agent"));
  }

  String getTerracottaTimType() {
    return TerracottaManagement.quoteIfNecessary(resources.getString("type.tim"));
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

  String getLoggingSubsystem() {
    return TerracottaManagement.quoteIfNecessary(resources.getString("subsystem.logging"));
  }

  String getStatisticsSubsystem() {
    return TerracottaManagement.quoteIfNecessary(resources.getString("subsystem.statistics"));
  }

  String getNoneSubsystem() {
    return TerracottaManagement.quoteIfNecessary(resources.getString("subsystem.none"));
  }

  public String getTerracottaOperatorEventType() {
    return TerracottaManagement.quoteIfNecessary(resources.getString("type.tc-operator-events"));
  }
}
