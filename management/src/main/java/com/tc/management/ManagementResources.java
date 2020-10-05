/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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

  String getNodeNameSystemProperty() {
    return resources.getString("system-property.node-name");
  }

  String getDsoClientType() {
    return TerracottaManagement.quoteIfNecessary(resources.getString("type.client"));
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
}