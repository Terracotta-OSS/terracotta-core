/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.impl;

import com.tc.entity.VoltronEntityMessage;
import com.tc.object.EntityID;
import com.tc.objectserver.entity.CreateSystemEntityMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.terracotta.config.Entities;
import org.terracotta.config.Entity;
import org.terracotta.config.TcConfig;
import org.w3c.dom.Element;

/**
 *
 */
public class PermanentEntityParser {
  public static List<VoltronEntityMessage> parseEntities(TcConfig config) {
      Entities e = config.getEntities();
      List<VoltronEntityMessage> msgs = null;
      if (e != null) {
        msgs = new ArrayList<>(e.getEntity().size());
        for (Entity b : e.getEntity()) {
          String name = b.getName();
          String type = b.getType();
          int version = b.getVersion();
          Entity.Configuration c = b.getConfiguration();
          Entity.Configuration.Properties m = c.getProperties();
          byte[] data;
          if (m != null) {
            Properties prop = new Properties();
            List<Element> list = m.getAny();
            for (Element pe : list) {
              prop.setProperty(pe.getTagName(), pe.getTextContent());
            }
            try {
              ByteArrayOutputStream bos = new ByteArrayOutputStream();
              prop.store(bos, null);
              data = bos.toByteArray();
            } catch (IOException ioe) {
              data = new byte[0];
            }
          } else {
            Element any = c.getAny();
            try {
              TransformerFactory transFactory = TransformerFactory.newInstance();
              Transformer transformer = transFactory.newTransformer();
              StringWriter buffer = new StringWriter();
              transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
              transformer.transform(new DOMSource(any), new StreamResult(buffer));
              String str = buffer.toString();
              data = str.getBytes();
            } catch (TransformerException te) {
              data = new byte[0];
            }
          }
          msgs.add(new CreateSystemEntityMessage(new EntityID(type, name),version, data));
        } 
      } else {
        msgs = Collections.emptyList();
      }
      return msgs;
  }
}
