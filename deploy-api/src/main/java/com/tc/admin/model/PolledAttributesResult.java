/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.model;

import java.util.Map;

import javax.management.ObjectName;

public interface PolledAttributesResult {
  Map<ObjectName, Map<String, Object>> getAttributeMap(IClusterNode clusterNode);
  Object getPolledAttribute(IClusterNode clusterNode, ObjectName objectName, String attribute);
  Object getPolledAttribute(IClusterNode clusterNode, PolledAttribute polledAttribute);
  Object getPolledAttribute(IClusterNode clusterNode, String name);
  
}
