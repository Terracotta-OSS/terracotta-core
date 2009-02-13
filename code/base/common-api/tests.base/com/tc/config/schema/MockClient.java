/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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

import com.tc.exception.ImplementMe;
import com.terracottatech.config.Client;
import com.terracottatech.config.DsoClientData;
import com.terracottatech.config.Modules;
import com.terracottatech.config.Path;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

/**
 * A mock {@link Client}, for use in tests.
 */
public class MockClient implements Client {

  public MockClient() {
    super();
  }

  public String getLogs() {
    throw new ImplementMe();
  }

  public Path xgetLogs() {
    throw new ImplementMe();
  }

  public boolean isSetLogs() {
    throw new ImplementMe();
  }

  public void setLogs(String arg0) {
    throw new ImplementMe();
  }

  public void xsetLogs(Path arg0) {
    throw new ImplementMe();
  }

  public void unsetLogs() {
    throw new ImplementMe();
  }

  public DsoClientData getDso() {
    throw new ImplementMe();
  }

  public boolean isSetDso() {
    throw new ImplementMe();
  }

  public void setDso(DsoClientData arg0) {
    throw new ImplementMe();

  }

  public DsoClientData addNewDso() {
    throw new ImplementMe();
  }

  public void unsetDso() {
    throw new ImplementMe();

  }

  public SchemaType schemaType() {
    throw new ImplementMe();
  }

  public boolean validate() {
    throw new ImplementMe();
  }

  public boolean validate(XmlOptions arg0) {
    throw new ImplementMe();
  }

  public XmlObject[] selectPath(String arg0) {
    throw new ImplementMe();
  }

  public XmlObject[] selectPath(String arg0, XmlOptions arg1) {
    throw new ImplementMe();
  }

  public XmlObject[] execQuery(String arg0) {
    throw new ImplementMe();
  }

  public XmlObject[] execQuery(String arg0, XmlOptions arg1) {
    throw new ImplementMe();
  }

  public XmlObject changeType(SchemaType arg0) {
    throw new ImplementMe();
  }

  public XmlObject substitute(QName arg0, SchemaType arg1) {
    throw new ImplementMe();
  }

  public boolean isNil() {
    throw new ImplementMe();
  }

  public void setNil() {
    throw new ImplementMe();

  }

  public boolean isImmutable() {
    throw new ImplementMe();
  }

  public XmlObject set(XmlObject arg0) {
    throw new ImplementMe();
  }

  public XmlObject copy() {
    throw new ImplementMe();
  }

  public boolean valueEquals(XmlObject arg0) {
    throw new ImplementMe();
  }

  public int valueHashCode() {
    throw new ImplementMe();
  }

  public int compareTo(Object arg0) {
    throw new ImplementMe();
  }

  public int compareValue(XmlObject arg0) {
    throw new ImplementMe();
  }

  public XmlObject[] selectChildren(QName arg0) {
    throw new ImplementMe();
  }

  public XmlObject[] selectChildren(String arg0, String arg1) {
    throw new ImplementMe();
  }

  public XmlObject[] selectChildren(QNameSet arg0) {
    throw new ImplementMe();
  }

  public XmlObject selectAttribute(QName arg0) {
    throw new ImplementMe();
  }

  public XmlObject selectAttribute(String arg0, String arg1) {
    throw new ImplementMe();
  }

  public XmlObject[] selectAttributes(QNameSet arg0) {
    throw new ImplementMe();
  }

  public Object monitor() {
    throw new ImplementMe();
  }

  public XmlDocumentProperties documentProperties() {
    throw new ImplementMe();
  }

  public XmlCursor newCursor() {
    throw new ImplementMe();
  }

  public XMLInputStream newXMLInputStream() {
    throw new ImplementMe();
  }

  public XMLStreamReader newXMLStreamReader() {
    throw new ImplementMe();
  }

  public String xmlText() {
    throw new ImplementMe();
  }

  public InputStream newInputStream() {
    throw new ImplementMe();
  }

  public Reader newReader() {
    throw new ImplementMe();
  }

  public Node newDomNode() {
    throw new ImplementMe();
  }

  public Node getDomNode() {
    throw new ImplementMe();
  }

  public void save(ContentHandler arg0, LexicalHandler arg1) {
    throw new ImplementMe();
  }

  public void save(File arg0) {
    throw new ImplementMe();
  }

  public void save(OutputStream arg0) {
    throw new ImplementMe();
  }

  public void save(Writer arg0) {
    throw new ImplementMe();
  }

  public XMLInputStream newXMLInputStream(XmlOptions arg0) {
    throw new ImplementMe();
  }

  public XMLStreamReader newXMLStreamReader(XmlOptions arg0) {
    throw new ImplementMe();
  }

  public String xmlText(XmlOptions arg0) {
    throw new ImplementMe();
  }

  public InputStream newInputStream(XmlOptions arg0) {
    throw new ImplementMe();
  }

  public Reader newReader(XmlOptions arg0) {
    throw new ImplementMe();
  }

  public Node newDomNode(XmlOptions arg0) {
    throw new ImplementMe();
  }

  public void save(ContentHandler arg0, LexicalHandler arg1, XmlOptions arg2) {
    throw new ImplementMe();
  }

  public void save(File arg0, XmlOptions arg1) {
    throw new ImplementMe();
  }

  public void save(OutputStream arg0, XmlOptions arg1) {
    throw new ImplementMe();
  }

  public void save(Writer arg0, XmlOptions arg1) {
    throw new ImplementMe();
  }

  public void dump() {
    throw new ImplementMe();
  }

  public Modules addNewModules() {
    throw new ImplementMe();
  }

  public Modules getModules() {
    throw new ImplementMe();
  }

  public boolean isSetModules() {
    throw new ImplementMe();
  }

  public void setModules(Modules modules) {
    throw new ImplementMe();
  }

  public void unsetModules() {
    throw new ImplementMe();
  }

  public String getStatistics() {
    throw new ImplementMe();
  }

  public boolean isSetStatistics() {
    throw new ImplementMe();
  }

  public void setStatistics(String arg0) {
    throw new ImplementMe();
    
  }

  public void unsetStatistics() {
    throw new ImplementMe();
    
  }

  public Path xgetStatistics() {
    throw new ImplementMe();
  }

  public void xsetStatistics(Path arg0) {
    throw new ImplementMe();
  }
}
