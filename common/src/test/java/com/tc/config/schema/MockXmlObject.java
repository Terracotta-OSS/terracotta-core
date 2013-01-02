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

  @Override
  public XmlDocumentProperties documentProperties() {
    return null;
  }

  @Override
  public void dump() {
    // Nothing here
  }

  @Override
  public Node getDomNode() {
    return null;
  }

  @Override
  public Object monitor() {
    return null;
  }

  @Override
  public XmlCursor newCursor() {
    return null;
  }

  @Override
  public Node newDomNode() {
    return null;
  }

  @Override
  public Node newDomNode(XmlOptions arg0) {
    return null;
  }

  @Override
  public InputStream newInputStream() {
    return null;
  }

  @Override
  public InputStream newInputStream(XmlOptions arg0) {
    return null;
  }

  @Override
  public Reader newReader() {
    return null;
  }

  @Override
  public Reader newReader(XmlOptions arg0) {
    return null;
  }

  @Override
  public XMLInputStream newXMLInputStream() {
    return null;
  }

  @Override
  public XMLInputStream newXMLInputStream(XmlOptions arg0) {
    return null;
  }

  @Override
  public XMLStreamReader newXMLStreamReader() {
    return null;
  }

  @Override
  public XMLStreamReader newXMLStreamReader(XmlOptions arg0) {
    return null;
  }

  @Override
  public void save(ContentHandler arg0, LexicalHandler arg1, XmlOptions arg2) {
    // Nothing here
  }

  @Override
  public void save(ContentHandler arg0, LexicalHandler arg1) {
    // Nothing here
  }

  @Override
  public void save(File arg0, XmlOptions arg1) {
    // Nothing here
  }

  @Override
  public void save(File arg0) {
    // Nothing here
  }

  @Override
  public void save(OutputStream arg0, XmlOptions arg1) {
    // Nothing here
  }

  @Override
  public void save(OutputStream arg0) {
    // Nothing here
  }

  @Override
  public void save(Writer arg0, XmlOptions arg1) {
    // Nothing here
  }

  @Override
  public void save(Writer arg0) {
    // Nothing here
  }

  @Override
  public String xmlText() {
    return null;
  }

  @Override
  public String xmlText(XmlOptions arg0) {
    return null;
  }

  @Override
  public XmlObject changeType(SchemaType arg0) {
    return null;
  }

  @Override
  public int compareTo(Object arg0) {
    return 0;
  }

  @Override
  public int compareValue(XmlObject arg0) {
    return 0;
  }

  @Override
  public XmlObject copy() {
    return null;
  }

  @Override
  public XmlObject[] execQuery(String arg0, XmlOptions arg1) {
    return null;
  }

  @Override
  public XmlObject[] execQuery(String arg0) {
    return null;
  }

  @Override
  public boolean isImmutable() {
    return false;
  }

  @Override
  public boolean isNil() {
    return false;
  }

  @Override
  public SchemaType schemaType() {
    return null;
  }

  @Override
  public XmlObject selectAttribute(QName arg0) {
    return null;
  }

  @Override
  public XmlObject selectAttribute(String arg0, String arg1) {
    return null;
  }

  @Override
  public XmlObject[] selectAttributes(QNameSet arg0) {
    return null;
  }

  @Override
  public XmlObject[] selectChildren(QName arg0) {
    return null;
  }

  @Override
  public XmlObject[] selectChildren(QNameSet arg0) {
    return null;
  }

  @Override
  public XmlObject[] selectChildren(String arg0, String arg1) {
    return null;
  }

  @Override
  public XmlObject[] selectPath(String arg0, XmlOptions arg1) {
    return null;
  }

  @Override
  public XmlObject[] selectPath(String arg0) {
    ++this.numSelectPaths;
    this.lastSelectPath = arg0;
    return this.returnedSelectPath;
  }

  @Override
  public XmlObject set(XmlObject arg0) {
    return null;
  }

  @Override
  public void setNil() {
    // Nothing here
  }

  @Override
  public XmlObject substitute(QName arg0, SchemaType arg1) {
    return null;
  }

  @Override
  public boolean validate() {
    return false;
  }

  @Override
  public boolean validate(XmlOptions arg0) {
    return false;
  }

  @Override
  public boolean valueEquals(XmlObject arg0) {
    return false;
  }

  @Override
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
