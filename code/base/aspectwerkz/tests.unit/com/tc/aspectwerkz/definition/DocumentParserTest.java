/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.definition;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.tc.aspectwerkz.DeploymentModel;

import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

/**
 * @author Eugene Kuleshov
 */
public class DocumentParserTest extends TestCase {

  public void testParse() throws Exception {
    // URL resource = getClass().getResource(getClass().getName().replace('.', '/') + ".xml");
    URL resource = getClass().getResource("DocumentParserTest.xml");
    assertNotNull(resource);
    InputStream is = resource.openStream(); 
    
    Set definitions = DocumentParser.parse(getClass().getClassLoader(), XmlParser.createDocument(new InputSource(is)));
    
    assertTrue(!definitions.isEmpty());
  }
  
  public void testParseAspectDefinition() {
    Class c = FooAspect.class;
    Class cc = com.tc.aspectwerkz.aspect.DefaultAspectContainerStrategy.class;

    String xmlDef = "<aspect class=\"" + c.getName() + "\" " 
        + "name=\"foo\" " 
        + "deployment-model=\"perJVM\" "
        + "container=\"" + cc.getName() + "\"></aspect>";
    
    SystemDefinition systemDef = SystemDefinitionContainer.getVirtualDefinitionFor(getClass().getClassLoader());
    AspectDefinition definition = DocumentParser.parseAspectDefinition(xmlDef, systemDef, c);
    
    assertEquals(c.getName(), definition.getClassName());
    assertEquals(cc.getName(), definition.getContainerClassName());
    assertEquals("foo", definition.getName());
    assertEquals(DeploymentModel.PER_JVM, definition.getDeploymentModel());
  }
  
  public void testParseAspectClassNames() throws Exception {
    String xml = "<aspectwerkz>" +
        "   <system base-package=\"bla.bla.bla\">  </system>" +
        "<system>" +
        "  <aspect class=\"aaa1\"/>" +
        "  <aspect class=\"aaa2\"/>" +
        "</system>  " +
        "<!-- test -->" +
        "<system>" +
        "  <aspect class=\"aaa3\"/>" +
        "  <package><aspect class=\"ppp1\"/></package>" +
        "</system>  " +
        "<system>" +
        "  <package><aspect class=\"ppp2\"/></package>" +
        "</system>  " +
        "</aspectwerkz>";
    
    List classNames = DocumentParser.parseAspectClassNames(getDocument(new InputSource(new StringReader(xml))));
    assertEquals("Error: " + classNames, 6, classNames.size());
  }
  
  public void testGetText() throws Exception {
    String xml = "<aspectwerkz>  " +
    "   <system> aaaa </system>" +
    "</aspectwerkz>";

    Document document = getDocument(new InputSource(new StringReader(xml)));
    NodeList elements = document.getElementsByTagName("system");
    String text = DocumentParser.getText((Element) elements.item(0));
    assertEquals("aaaa", text.trim());
  }
  
  private static Document getDocument(InputSource is) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    return builder.parse(is);
  }
  
  
  public static class FooAspect {
    public Object addRequestTag() {
      return null;
    }
  }

}
