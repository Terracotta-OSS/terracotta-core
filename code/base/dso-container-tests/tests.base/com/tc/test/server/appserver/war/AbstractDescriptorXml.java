/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.war;

/**
 * Handles the formatting of XML elements.
 */
public abstract class AbstractDescriptorXml implements DescriptorXml {

  protected static final String INDENT = "  ";
  private final StringBuffer    sout;

  protected AbstractDescriptorXml() {
    this.sout = new StringBuffer();
  }

  public abstract byte[] getBytes();

  public abstract String getFileName();

  public String toString() {
    return new String(getBytes());
  }

  public static String translateUrl(String servletName) {
    return servletName.replace('$', '-');
  }

  protected String indent(int level) {
    String indent = "";
    for (int i = 0; i < level; i++)
      indent += INDENT;
    return indent;
  }

  protected void add(String txt) {
    sout.append(txt + "\n");
  }

  protected void add(int pad, String txt) {
    sout.append(indent(pad) + txt + "\n");
  }

  protected StringBuffer sout() {
    return sout;
  }
}
