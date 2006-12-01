/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.war;

import java.util.Map;

/**
 * Represents a WAR compliant web.xml descriptor file.
 */
public interface WebXml extends DescriptorXml {

  // returns URL pattern for this servlet (not a full URL)
  String addServlet(Class servletClass);
  void addListener(Class listenerClass);
  void addFilter(Class filterClass, String pattern, Map initParams);
}
