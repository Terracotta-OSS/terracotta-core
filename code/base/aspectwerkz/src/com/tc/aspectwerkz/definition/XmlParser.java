/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.definition;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.tc.aspectwerkz.exception.WrappedRuntimeException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Parses the XML definition file using <tt>dom4j</tt>.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public class XmlParser {
  /**
   * The current DTD public id. The matching dtd will be searched as a resource.
   */
  private final static String DTD_PUBLIC_ID = "-//AspectWerkz//DTD 2.0//EN";

  /**
   * The DTD alias, for better user experience.
   */
  private final static String DTD_PUBLIC_ID_ALIAS = "-//AspectWerkz//DTD//EN";

  /**
   * A handler to the DTD stream so that we are only using one file descriptor
   */
  private final static URL DTD_URL = XmlParser.class.getResource("/aspectwerkz2.dtd");

  /**
   * The AspectWerkz definitions.
   */
  private static Set s_definitions = null;

  /**
   * Parses the XML definition file not using the cache.
   *
   * @param loader the current class loader
   * @param url    the URL to the definition file
   * @return the definition object
   */
  public static Set parseNoCache(final ClassLoader loader, final URL url) {
    try {
      // XXX: AspectJ aop.xml parsing is done like this:
      // Definition definition = DocumentParser.parse(url);
      // System.out.println("------------------> " + definition);

      s_definitions = DocumentParser.parse(loader, createDocument(url));
      return s_definitions;
    } catch (Exception e) {
      throw new WrappedRuntimeException(e);
    }
  }

  /**
   * Merges two DOM documents.
   *
   * @param document1 the first document
   * @param document2 the second document
   * @return the definition merged document
   */
//  public static Document mergeDocuments(final Document document1, final Document document2) {
//    if ((document2 == null) && (document1 != null)) {
//      return document1;
//    }
//    if ((document1 == null) && (document2 != null)) {
//      return document2;
//    }
//    if ((document1 == null) && (document2 == null)) {
//      return null;
//    }
//    try {
//      Element root1 = document1.getRootElement();
//      Element root2 = document2.getRootElement();
//      for (Iterator it1 = root2.elementIterator(); it1.hasNext();) {
//        Element element = (Element) it1.next();
//        element.setParent(null);
//        root1.add(element);
//      }
//    } catch (Exception e) {
//      throw new WrappedRuntimeException(e);
//    }
//    return document1;
//  }

  /**
   * Creates a DOM document.
   *
   * @param url the URL to the file containing the XML
   * @return the DOM document
   * 
   * @throws IOException 
   * @throws SAXException 
   * @throws ParserConfigurationException 
   */
  public static Document createDocument(final URL url) throws IOException {
    InputStream in = null;
    try {
      in = url.openStream();
      return createDocument(new InputSource(in));
    } finally {
      try {
        in.close();
      } catch (Throwable t) {
        // ignore
      }
    }
  }

  /**
   * Creates a DOM document.
   *
   * @param string the string containing the XML
   * @return the DOM document
   * @throws DocumentException
   */
  public static Document createDocument(final String string) throws IOException {
    return createDocument(new InputSource(new StringReader(string)));
  }

  public static Document createDocument(InputSource in) throws IOException {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.setEntityResolver(getEntityResolver());
      return builder.parse(in);
    } catch (SAXException e) {
      throw new IOException(e.toString());
    } catch (ParserConfigurationException e) {
      throw new IOException(e.toString());
    }      
  }
  

  /**
   * Sets the entity resolver which is created based on the DTD from in the root dir of the AspectWerkz distribution.
   *
   * @param reader the reader to set the resolver in
   */
  private static EntityResolver getEntityResolver() {
    return new EntityResolver() {
        public InputSource resolveEntity(String publicId, String systemId) {
          if (publicId.equals(DTD_PUBLIC_ID) || publicId.equals(DTD_PUBLIC_ID_ALIAS)) {
            try {
              InputStream in = DTD_URL.openStream();
              if (in != null)
                return new InputSource(in);
            } catch (IOException ioex) {
            }
            System.err.println("AspectWerkz - WARN - could not open DTD");
            return new InputSource(); // avoid null pointer exception }
          } else {
            System.err.println(
                    "AspectWerkz - WARN - deprecated DTD " + publicId +
                            " - consider upgrading to " + DTD_PUBLIC_ID);
            return new InputSource(); // avoid null pointer exception
          }
        }
      };
  }
}