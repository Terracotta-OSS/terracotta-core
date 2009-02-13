/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.QNameSet;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlDocumentProperties;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.xml.stream.XMLInputStream;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.ext.LexicalHandler;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

/**
 * A mock {@link XmlObject}, for use in tests.
 */
public class MockXmlObject implements XmlObject {

  public static final SchemaType type = new MockSchemaType();
  
  private int         numSelectPaths;
  private String      lastSelectPath;
  private XmlObject[] returnedSelectPath;

  public MockXmlObject() {
    reset();

    this.returnedSelectPath = null;
  }

  public void reset() {
    this.numSelectPaths = 0;
    this.lastSelectPath = null;
  }

  public XmlDocumentProperties documentProperties() {
    return null;
  }

  public void dump() {
    // Nothing here
  }

  public Node getDomNode() {
    return null;
  }

  public Object monitor() {
    return null;
  }

  public XmlCursor newCursor() {
    return null;
  }

  public Node newDomNode() {
    return null;
  }

  public Node newDomNode(XmlOptions arg0) {
    return null;
  }

  public InputStream newInputStream() {
    return null;
  }

  public InputStream newInputStream(XmlOptions arg0) {
    return null;
  }

  public Reader newReader() {
    return null;
  }

  public Reader newReader(XmlOptions arg0) {
    return null;
  }

  public XMLInputStream newXMLInputStream() {
    return null;
  }

  public XMLInputStream newXMLInputStream(XmlOptions arg0) {
    return null;
  }

  public XMLStreamReader newXMLStreamReader() {
    return null;
  }

  public XMLStreamReader newXMLStreamReader(XmlOptions arg0) {
    return null;
  }

  public void save(ContentHandler arg0, LexicalHandler arg1, XmlOptions arg2) {
    // Nothing here
  }

  public void save(ContentHandler arg0, LexicalHandler arg1) {
    // Nothing here
  }

  public void save(File arg0, XmlOptions arg1) {
    // Nothing here
  }

  public void save(File arg0) {
    // Nothing here
  }

  public void save(OutputStream arg0, XmlOptions arg1) {
    // Nothing here
  }

  public void save(OutputStream arg0) {
    // Nothing here
  }

  public void save(Writer arg0, XmlOptions arg1) {
    // Nothing here
  }

  public void save(Writer arg0) {
    // Nothing here
  }

  public String xmlText() {
    return null;
  }

  public String xmlText(XmlOptions arg0) {
    return null;
  }

  public XmlObject changeType(SchemaType arg0) {
    return null;
  }

  public int compareTo(Object arg0) {
    return 0;
  }

  public int compareValue(XmlObject arg0) {
    return 0;
  }

  public XmlObject copy() {
    return null;
  }

  public XmlObject[] execQuery(String arg0, XmlOptions arg1) {
    return null;
  }

  public XmlObject[] execQuery(String arg0) {
    return null;
  }

  public boolean isImmutable() {
    return false;
  }

  public boolean isNil() {
    return false;
  }

  public SchemaType schemaType() {
    return null;
  }

  public XmlObject selectAttribute(QName arg0) {
    return null;
  }

  public XmlObject selectAttribute(String arg0, String arg1) {
    return null;
  }

  public XmlObject[] selectAttributes(QNameSet arg0) {
    return null;
  }

  public XmlObject[] selectChildren(QName arg0) {
    return null;
  }

  public XmlObject[] selectChildren(QNameSet arg0) {
    return null;
  }

  public XmlObject[] selectChildren(String arg0, String arg1) {
    return null;
  }

  public XmlObject[] selectPath(String arg0, XmlOptions arg1) {
    return null;
  }

  public XmlObject[] selectPath(String arg0) {
    ++this.numSelectPaths;
    this.lastSelectPath = arg0;
    return this.returnedSelectPath;
  }

  public XmlObject set(XmlObject arg0) {
    return null;
  }

  public void setNil() {
    // Nothing here
  }

  public XmlObject substitute(QName arg0, SchemaType arg1) {
    return null;
  }

  public boolean validate() {
    return false;
  }

  public boolean validate(XmlOptions arg0) {
    return false;
  }

  public boolean valueEquals(XmlObject arg0) {
    return false;
  }

  public int valueHashCode() {
    return 0;
  }

  public String getLastSelectPath() {
    return lastSelectPath;
  }

  public int getNumSelectPaths() {
    return numSelectPaths;
  }

  public void setReturnedSelectPath(XmlObject[] returnedSelectPath) {
    this.returnedSelectPath = returnedSelectPath;
  }

}
