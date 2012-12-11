/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.QNameSet;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlBoolean;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlDocumentProperties;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.xml.stream.XMLInputStream;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.ext.LexicalHandler;

import com.tc.exception.ImplementMe;
import com.terracottatech.config.GarbageCollection;
import com.terracottatech.config.MirrorGroup;
import com.terracottatech.config.NonNegativeInt;
import com.terracottatech.config.Restartable;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.UpdateCheck;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

/**
 * A mock {@link Servers}, for use in tests.
 */
public class MockServers implements Servers {

  public MockServers() {
    super();
  }

  @Override
  public boolean getSecure() {
    throw new ImplementMe();
  }

  @Override
  public XmlBoolean xgetSecure() {
    throw new ImplementMe();
  }

  @Override
  public boolean isSetSecure() {
    throw new ImplementMe();
  }

  @Override
  public void setSecure(boolean secure) {
    throw new ImplementMe();
  }

  @Override
  public void xsetSecure(XmlBoolean secure) {
    throw new ImplementMe();
  }

  @Override
  public void unsetSecure() {
    throw new ImplementMe();
  }

  @Override
  public SchemaType schemaType() {
    throw new ImplementMe();
  }

  @Override
  public boolean validate() {
    throw new ImplementMe();
  }

  @Override
  public boolean validate(XmlOptions arg0) {
    throw new ImplementMe();
  }

  @Override
  public XmlObject[] selectPath(String arg0) {
    throw new ImplementMe();
  }

  @Override
  public XmlObject[] selectPath(String arg0, XmlOptions arg1) {
    throw new ImplementMe();
  }

  @Override
  public XmlObject[] execQuery(String arg0) {
    throw new ImplementMe();
  }

  @Override
  public XmlObject[] execQuery(String arg0, XmlOptions arg1) {
    throw new ImplementMe();
  }

  @Override
  public XmlObject changeType(SchemaType arg0) {
    throw new ImplementMe();
  }

  @Override
  public XmlObject substitute(QName arg0, SchemaType arg1) {
    throw new ImplementMe();
  }

  @Override
  public boolean isNil() {
    throw new ImplementMe();
  }

  @Override
  public void setNil() {
    throw new ImplementMe();
  }

  @Override
  public boolean isImmutable() {
    throw new ImplementMe();
  }

  @Override
  public XmlObject set(XmlObject arg0) {
    throw new ImplementMe();
  }

  @Override
  public XmlObject copy() {
    throw new ImplementMe();
  }

  @Override
  public boolean valueEquals(XmlObject arg0) {
    throw new ImplementMe();
  }

  @Override
  public int valueHashCode() {
    throw new ImplementMe();
  }

  @Override
  public int compareTo(Object arg0) {
    throw new ImplementMe();
  }

  @Override
  public int compareValue(XmlObject arg0) {
    throw new ImplementMe();
  }

  @Override
  public XmlObject[] selectChildren(QName arg0) {
    throw new ImplementMe();
  }

  @Override
  public XmlObject[] selectChildren(String arg0, String arg1) {
    throw new ImplementMe();
  }

  @Override
  public XmlObject[] selectChildren(QNameSet arg0) {
    throw new ImplementMe();
  }

  @Override
  public XmlObject selectAttribute(QName arg0) {
    throw new ImplementMe();
  }

  @Override
  public XmlObject selectAttribute(String arg0, String arg1) {
    throw new ImplementMe();
  }

  @Override
  public XmlObject[] selectAttributes(QNameSet arg0) {
    throw new ImplementMe();
  }

  @Override
  public Object monitor() {
    throw new ImplementMe();
  }

  @Override
  public XmlDocumentProperties documentProperties() {
    throw new ImplementMe();
  }

  @Override
  public XmlCursor newCursor() {
    throw new ImplementMe();
  }

  @Override
  public XMLInputStream newXMLInputStream() {
    throw new ImplementMe();
  }

  @Override
  public XMLStreamReader newXMLStreamReader() {
    throw new ImplementMe();
  }

  @Override
  public String xmlText() {
    throw new ImplementMe();
  }

  @Override
  public InputStream newInputStream() {
    throw new ImplementMe();
  }

  @Override
  public Reader newReader() {
    throw new ImplementMe();
  }

  @Override
  public Node newDomNode() {
    throw new ImplementMe();
  }

  @Override
  public Node getDomNode() {
    throw new ImplementMe();
  }

  @Override
  public void save(ContentHandler arg0, LexicalHandler arg1) {
    throw new ImplementMe();
  }

  @Override
  public void save(File arg0) {
    throw new ImplementMe();
  }

  @Override
  public void save(OutputStream arg0) {
    throw new ImplementMe();
  }

  @Override
  public void save(Writer arg0) {
    throw new ImplementMe();
  }

  @Override
  public XMLInputStream newXMLInputStream(XmlOptions arg0) {
    throw new ImplementMe();
  }

  @Override
  public XMLStreamReader newXMLStreamReader(XmlOptions arg0) {
    throw new ImplementMe();
  }

  @Override
  public String xmlText(XmlOptions arg0) {
    throw new ImplementMe();
  }

  @Override
  public InputStream newInputStream(XmlOptions arg0) {
    throw new ImplementMe();
  }

  @Override
  public Reader newReader(XmlOptions arg0) {
    throw new ImplementMe();
  }

  @Override
  public Node newDomNode(XmlOptions arg0) {
    throw new ImplementMe();
  }

  @Override
  public void save(ContentHandler arg0, LexicalHandler arg1, XmlOptions arg2) {
    throw new ImplementMe();
  }

  @Override
  public void save(File arg0, XmlOptions arg1) {
    throw new ImplementMe();
  }

  @Override
  public void save(OutputStream arg0, XmlOptions arg1) {
    throw new ImplementMe();
  }

  @Override
  public void save(Writer arg0, XmlOptions arg1) {
    throw new ImplementMe();
  }

  @Override
  public void dump() {
    throw new ImplementMe();
  }

  @Override
  public UpdateCheck addNewUpdateCheck() {
    throw new ImplementMe();
  }

  @Override
  public UpdateCheck getUpdateCheck() {
    throw new ImplementMe();
  }

  @Override
  public boolean isSetUpdateCheck() {
    throw new ImplementMe();
  }

  @Override
  public void setUpdateCheck(UpdateCheck arg0) {
    throw new ImplementMe();
  }

  @Override
  public void unsetUpdateCheck() {
    throw new ImplementMe();
  }

  @Override
  public int getClientReconnectWindow() {
    throw new ImplementMe();
  }

  @Override
  public NonNegativeInt xgetClientReconnectWindow() {
    throw new ImplementMe();
  }

  @Override
  public boolean isSetClientReconnectWindow() {
    throw new ImplementMe();
  }

  @Override
  public void setClientReconnectWindow(int clientReconnectWindow) {
    throw new ImplementMe();
  }

  @Override
  public void xsetClientReconnectWindow(NonNegativeInt clientReconnectWindow) {
    throw new ImplementMe();
  }

  @Override
  public void unsetClientReconnectWindow() {
    throw new ImplementMe();
  }

  @Override
  public Restartable getRestartable() {
    throw new ImplementMe();
  }

  @Override
  public boolean isSetRestartable() {
    throw new ImplementMe();
  }

  @Override
  public void setRestartable(Restartable restartable) {
    throw new ImplementMe();
  }

  @Override
  public Restartable addNewRestartable() {
    throw new ImplementMe();
  }

  @Override
  public void unsetRestartable() {
    throw new ImplementMe();
  }

  @Override
  public GarbageCollection getGarbageCollection() {
    throw new ImplementMe();
  }

  @Override
  public boolean isSetGarbageCollection() {
    throw new ImplementMe();
  }

  @Override
  public void setGarbageCollection(GarbageCollection garbageCollection) {
    throw new ImplementMe();

  }

  @Override
  public GarbageCollection addNewGarbageCollection() {
    throw new ImplementMe();
  }

  @Override
  public void unsetGarbageCollection() {
    throw new ImplementMe();
  }

  @Override
  public MirrorGroup[] getMirrorGroupArray() {
    throw new ImplementMe();
  }

  @Override
  public MirrorGroup getMirrorGroupArray(int i) {
    throw new ImplementMe();
  }

  @Override
  public int sizeOfMirrorGroupArray() {
    throw new ImplementMe();
  }

  @Override
  public void setMirrorGroupArray(MirrorGroup[] mirrorGroupArray) {
    throw new ImplementMe();
  }

  @Override
  public void setMirrorGroupArray(int i, MirrorGroup mirrorGroup) {
    throw new ImplementMe();
  }

  @Override
  public MirrorGroup insertNewMirrorGroup(int i) {
    throw new ImplementMe();
  }

  @Override
  public MirrorGroup addNewMirrorGroup() {
    throw new ImplementMe();
  }

  @Override
  public void removeMirrorGroup(int i) {
    throw new ImplementMe();
  }

  @Override
  public Server[] getServerArray() {
    throw new ImplementMe();
  }

  @Override
  public Server getServerArray(int i) {
    throw new ImplementMe();
  }

  @Override
  public int sizeOfServerArray() {
    throw new ImplementMe();
  }

  @Override
  public void setServerArray(Server[] serverArray) {
    throw new ImplementMe();
  }

  @Override
  public void setServerArray(int i, Server server) {
    throw new ImplementMe();
  }

  @Override
  public Server insertNewServer(int i) {
    throw new ImplementMe();
  }

  @Override
  public Server addNewServer() {
    throw new ImplementMe();
  }

  @Override
  public void removeServer(int i) {
    throw new ImplementMe();
  }
}
