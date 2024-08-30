/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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