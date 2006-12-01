/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.war;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Implementers are able to able to generate and write a WAR (Web Application Resource) to the filesystem.
 */
public interface War {

  // returns URL pattern for this servlet (not a full URL)
  String addServlet(Class ServletClass);

  // adds listener spec to web.xml in this war file
  void addListener(Class listenerClass);
  
  // adds Servlet filter to web.xml in this war file
  void addFilter(Class filterClass, String pattern, Map initParams);
  
  void addClass(Class clazz);
 
  // jar file or classes directory (at com.* level)
  void addLibrary(File lib);

  void addContainerSpecificXml(String fileName, byte[] containerXml);

  // returns file name
  String writeWarFileToDirectory(File directory) throws IOException;

}
