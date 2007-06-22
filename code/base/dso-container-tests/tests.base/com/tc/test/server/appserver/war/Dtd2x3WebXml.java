/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.war;

import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Knows the format of a dtd 2.3 compliant web.xml file as part of the J2EE 2.0 WAR spec. The relevant feature of this
 * class is it's ability to map servlet classes to a URL by passing a class object. The actual URL to a given servlet
 * class is mapped to the domain:port/[app name]/[servlet url]. To obtain a servlet URL you must call the static method
 * {@link AbstractDescriptorXml.translateUrl()} passing just the class name (not the full pathname).
 */
public final class Dtd2x3WebXml extends AbstractDescriptorXml implements WebXml {

  private final static String NAME = "web.xml";
  private final List          servlets;
  private final List          listeners;
  private final List          filters;
  private final String        appName;

  public Dtd2x3WebXml(String appName) {
    super();
    Assert.assertTrue(appName != null);
    this.appName = appName;
    this.servlets = new ArrayList();
    this.listeners = new ArrayList();
    this.filters = new ArrayList();
  }

  public String addServlet(Class servletClass) {
    String name = servletClass.getName();
    String[] parts = name.split("\\.");
    servlets.add(name);
    return translateUrl(parts[parts.length - 1]);
  }

  public void addListener(Class listenerClass) {
    String name = listenerClass.getName();
    listeners.add(name);
  }

  public void addFilter(Class filterClass, String pattern, Map initParams) {
    Filter filter = new Filter();
    filter.filterClass = filterClass;
    filter.pattern = pattern;
    filter.initParams = initParams;
    filters.add(filter);
  }

  public byte[] getBytes() {
    add("<?xml version=\"1.0\"?>");
    add("<!DOCTYPE web-app PUBLIC \"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN\" \"http://java.sun.com/dtd/web-app_2_3.dtd\">");
    add("");
    add("<web-app>");
    add(1, "<display-name>" + appName + "</display-name>");
    addFilters();
    addListeners();
    addServlets();
    addMappings();
    add("</web-app>");

    return sout().toString().getBytes();
  }

  public String getFileName() {
    return NAME;
  }

  private void addListeners() {
    for (Iterator iter = listeners.iterator(); iter.hasNext();) {
      String className = (String) iter.next();
      add(1, "<listener><listener-class>" + className + "</listener-class></listener>");
    }
  }

  private void addServlets() {
    String servletName;
    for (Iterator iter = servlets.iterator(); iter.hasNext();) {
      servletName = (String) iter.next();
      String name = getSimpleName(servletName);
      add(1, "<servlet>");
      add(2, "<servlet-name>" + name + "</servlet-name>");
      add(2, "<servlet-class>" + servletName + "</servlet-class>");
      add(1, "</servlet>");
    }
  }

  private void addMappings() {
    for (Iterator iter = servlets.iterator(); iter.hasNext();) {
      String name = getSimpleName((String) iter.next());
      add(1, "<servlet-mapping>");
      add(2, "<servlet-name>" + name + "</servlet-name>");
      add(2, "<url-pattern>/" + translateUrl(name) + "</url-pattern>");
      add(1, "</servlet-mapping>");
    }
  }

  private void addFilters() {
    Filter filter;
    for (Iterator iter = filters.iterator(); iter.hasNext();) {
      add(1, "<filter>");
      filter = (Filter) iter.next();
      String name = getSimpleName(filter.filterClass.getName());
      add(2, "<filter-name>" + name + "</filter-name>");
      add(2, "<filter-class>" + filter.filterClass.getName() + "</filter-class>");
      if (filter.initParams != null) {
        add(2, "<init-param>");
        for (Iterator paramIter = filter.initParams.entrySet().iterator(); paramIter.hasNext();) {
          Map.Entry entry = (Map.Entry) paramIter.next();
          add(3, "<param-name>" + entry.getKey() + "</param-name>");
          add(3, "<param-value>" + entry.getValue() + "</param-value>");
        }
        add(2, "</init-param>");
      }
      add(1, "</filter>");
    }

    for (Iterator iter = filters.iterator(); iter.hasNext();) {
      add(1, "<filter-mapping>");
      filter = (Filter) iter.next();
      String name = getSimpleName(filter.filterClass.getName());
      add(2, "<filter-name>" + name + "</filter-name>");
      add(2, "<url-pattern>" + filter.pattern + "</url-pattern>");
      add(1, "</filter-mapping>");
    }
  }

  private String getSimpleName(String className) {
    String[] parts = className.split("\\.");
    return parts[parts.length - 1];
  }

  private class Filter {
    Class  filterClass;
    String pattern;
    Map    initParams;
  }
}
