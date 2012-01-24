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
import com.terracottatech.config.Ha;
import com.terracottatech.config.MirrorGroups;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.UpdateCheck;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

/**
 * A mock {@link Servers}, for use in tests.
 */
public class MockServers implements Servers {

  public MockServers() {
    super();
  }

  public Server[] getServerArray() {
    throw new ImplementMe();
  }

  public Server getServerArray(int arg0) {
    throw new ImplementMe();
  }

  public int sizeOfServerArray() {
    return 1;
  }

  public void setServerArray(Server[] arg0) {
    throw new ImplementMe();
  }

  public void setServerArray(int arg0, Server arg1) {
    throw new ImplementMe();
  }

  public Server insertNewServer(int arg0) {
    throw new ImplementMe();
  }

  public Server addNewServer() {
    throw new ImplementMe();
  }

  public void removeServer(int arg0) {
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

  public Ha addNewHa() {
    throw new ImplementMe();
  }

  public MirrorGroups addNewMirrorGroups() {
    throw new ImplementMe();
  }

  public MirrorGroups[] getMirrorGroupsArray() {
    throw new ImplementMe();
  }

  public MirrorGroups getMirrorGroupsArray(int arg0) {
    throw new ImplementMe();
  }

  public Ha[] getHaArray() {
    throw new ImplementMe();
  }

  public Ha getHaArray(int arg0) {
    throw new ImplementMe();
  }

  public UpdateCheck addNewUpdateCheck() {
    throw new ImplementMe();
  }

  public MirrorGroups insertNewMirrorGroups(int arg0) {
    throw new ImplementMe();
  }

  public Ha insertNewHa(int arg0) {
    throw new ImplementMe();
  }

  public void removeActiveServerGroups(int arg0) {
    throw new ImplementMe();
  }

  public void removeHa(int arg0) {
    throw new ImplementMe();
  }

  public void setMirrorGroupsArray(MirrorGroups[] arg0) {
    throw new ImplementMe();
  }

  public void setActiveServerGroupsArray(int arg0, MirrorGroups arg1) {
    throw new ImplementMe();
  }

  public void setHaArray(Ha[] arg0) {
    throw new ImplementMe();
  }

  public void setHaArray(int arg0, Ha arg1) {
    throw new ImplementMe();
  }

  public int sizeOfActiveServerGroupsArray() {
    throw new ImplementMe();
  }

  public int sizeOfHaArray() {
    throw new ImplementMe();
  }

  public UpdateCheck[] getUpdateCheckArray() {
    throw new ImplementMe();
  }

  public UpdateCheck getUpdateCheckArray(int arg0) {
    throw new ImplementMe();
  }

  public UpdateCheck insertNewUpdateCheck(int arg0) {
    throw new ImplementMe();
  }

  public void removeUpdateCheck(int arg0) {
    throw new ImplementMe();
  }

  public void setUpdateCheckArray(UpdateCheck[] arg0) {
    throw new ImplementMe();
  }

  public void setUpdateCheckArray(int arg0, UpdateCheck arg1) {
    throw new ImplementMe();
  }

  public int sizeOfUpdateCheckArray() {
    throw new ImplementMe();
  }

  public MirrorGroups getMirrorGroups() {
    throw new ImplementMe();
  }

  public Ha getHa() {
    throw new ImplementMe();
  }

  public UpdateCheck getUpdateCheck() {
    throw new ImplementMe();
  }

  public boolean isSetMirrorGroups() {
    throw new ImplementMe();
  }

  public boolean isSetHa() {
    throw new ImplementMe();
  }

  public boolean isSetUpdateCheck() {
    throw new ImplementMe();
  }

  public void setMirrorGroups(MirrorGroups arg0) {
    throw new ImplementMe();

  }

  public void setHa(Ha arg0) {
    throw new ImplementMe();

  }

  public void setUpdateCheck(UpdateCheck arg0) {
    throw new ImplementMe();

  }

  public void unsetMirrorGroups() {
    throw new ImplementMe();

  }

  public void unsetHa() {
    throw new ImplementMe();

  }

  public void unsetUpdateCheck() {
    throw new ImplementMe();

  }

  public List<Server> getServerList() {
    throw new ImplementMe();
  }
}
