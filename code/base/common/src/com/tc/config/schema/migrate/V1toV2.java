/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.migrate;

import org.apache.commons.io.CopyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.StringEnumAbstractBase;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.terracottatech.configV1.Application;
import com.terracottatech.configV1.DsoApplication;
import com.terracottatech.configV1.DsoClientData;
import com.terracottatech.configV1.DsoServerData;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;

import javax.xml.namespace.QName;

/*
 * Converts from the V1 to the V2 configuration format.
 * 
 * The namespace has permanently changed from http://www.terracottatech.com/config.v1
 * to http://www.terracotta.org/config.
 * 
 * DSO and JMX are now required.
 * The following are gone:
 *  - JDBC
 *  - embedded HTTP server (Jetty) and HTTP interface to JMX
 *  - DsoClientData.maxInMemoryObjectCount
 *  - DsoServerData.serverCachedObjectCount
 *  - ConfigurationModel.DEMO
 *  - DSO ChangeListener
 */

public class V1toV2 {
  private static final String V1_NAMESPACE    = "http://www.terracottatech.com/config-v1";
  private static final String V2_NAMESPACE    = "http://www.terracotta.org/config";
  private static final String SCHEMA_LOCATION = "http://www.terracotta.org/schema/terracotta-2.2.xsd";
  private static final String XSI_NAMESPACE   = "http://www.w3.org/2001/XMLSchema-instance";
  
  protected XmlOptions defaultXmlOptions;
  private   boolean    addSchemaLocation;
  
  public V1toV2() {
    defaultXmlOptions = createDefaultXmlOptions();
    addSchemaLocation = false;
  }
  
  public com.terracottatech.config.TcConfigDocument
  convert(com.terracottatech.configV1.TcConfigDocument v1Doc)
    throws XmlException
  {
    return convert(v1Doc, defaultXmlOptions);
  }
  
  public com.terracottatech.config.TcConfigDocument
  convert(com.terracottatech.configV1.TcConfigDocument v1Doc, XmlOptions xmlOptions)
    throws XmlException
  {
    if(v1Doc != null) {
      com.terracottatech.configV1.TcConfigDocument.TcConfig v1Config = v1Doc.getTcConfig();
      
      if(v1Config.isSetSystem()) {
        com.terracottatech.configV1.System system = v1Config.getSystem();
        com.terracottatech.configV1.ConfigurationModel configModel = system.xgetConfigurationModel();
        
        if(configModel != null) {
          StringEnumAbstractBase value = configModel.enumValue();
          if(value.intValue() == com.terracottatech.configV1.ConfigurationModel.INT_DEMO) {
            configModel.set(com.terracottatech.configV1.ConfigurationModel.DEVELOPMENT);
          }
        }
        
        if(system.isSetDsoEnabled()) {
          system.unsetDsoEnabled();
        }
        if(system.isSetJdbcEnabled()) {
          system.unsetJdbcEnabled();
        }
        if(system.isSetHttpEnabled()) {
          system.unsetHttpEnabled();
        }
        if(system.isSetJmxEnabled()) {
          system.unsetJmxEnabled();
        }
        if(system.isSetJmxHttpEnabled()) {
          system.unsetJmxHttpEnabled();
        }
        
        if(!system.isSetConfigurationModel() && !system.isSetLicense()) {
          v1Config.unsetSystem();
        }
      }
      
      if(v1Config.isSetServers()) {
        com.terracottatech.configV1.Servers servers = v1Config.getServers();
        com.terracottatech.configV1.Server server;
        if(servers != null) {
          for(int i = 0; i < servers.sizeOfServerArray(); i++) {
            server = servers.getServerArray(i);
            
            if(server.isSetHttpPort()) {
              server.unsetHttpPort();
            }
            if(server.isSetJdbcPort()) {
              server.unsetJdbcPort();
            }
            if(server.isSetJmxHttpPort()) {
              server.unsetJmxHttpPort();
            }
            
            if(server.isSetDso()) {
              DsoServerData dsoServerData = server.getDso();
              
              if(dsoServerData.isSetServerCachedObjectCount()) {
                dsoServerData.unsetServerCachedObjectCount();
              }
              
              if(!dsoServerData.isSetClientReconnectWindow() &&
                 !dsoServerData.isSetGarbageCollection() &&
                 !dsoServerData.isSetPersistence()) {
                server.unsetDso();
              }
            }
          }
        }
      }
      
      if(v1Config.isSetClients()) {
        com.terracottatech.configV1.Client client = v1Config.getClients();
        if(client != null) {
           if(client.isSetDso()) {
            DsoClientData dsoClientData = client.getDso();
            
            if(dsoClientData.isSetMaxInMemoryObjectCount()) {
              dsoClientData.unsetMaxInMemoryObjectCount();
            }
            
            if(!dsoClientData.isSetDebugging() && !dsoClientData.isSetFaultCount()) {
              client.unsetDso();
            }
          }
        }
      }
      
      if(v1Config.isSetApplication()) {
        Application application = v1Config.getApplication();
        
        if(application.isSetJdbc()) {
          application.unsetJdbc();
        }
        if(application.isSetDso()) {
          DsoApplication dsoApp = application.getDso();
          
          if(dsoApp.isSetChangeListener()) {
            dsoApp.unsetChangeListener();
          }
        }
      }
      
      if(addSchemaLocation) {
        XmlCursor cursor = v1Doc.newCursor();
        if(cursor.toFirstChild()) {
          QName name = new QName(XSI_NAMESPACE, "schemaLocation");
          cursor.setAttributeText(name, SCHEMA_LOCATION);
        }
        cursor.dispose();
      }
      
      InputStream       inStream       = v1Doc.newInputStream(xmlOptions);
      InputStreamReader inReader       = new InputStreamReader(inStream);
      BufferedReader    bufferedReader = new BufferedReader(inReader);
      StringBuffer      sb             = new StringBuffer();
      String            nl             = System.getProperty("line.separator"); 
      String            s;

      try {
        while((s = bufferedReader.readLine()) != null) {
          sb.append(StringUtils.replace(s, V1_NAMESPACE, V2_NAMESPACE));
          sb.append(nl);
        }
      } catch(IOException ioe) {
        /* this won't happen because the source stream isn't file- or network-based */
      }

      return loadV2(sb.toString(), xmlOptions);
    }
    
    return null;
  }

  public com.terracottatech.config.TcConfigDocument
  convert(File file) throws IOException, XmlException {
    return convert(loadV1(file));
  }
  
  public com.terracottatech.config.TcConfigDocument
  convert(File file, XmlOptions xmlOptions) throws IOException, XmlException {
    return convert(loadV1(file, xmlOptions));
  }

  public com.terracottatech.config.TcConfigDocument
  convert(String xmlText) throws XmlException {
    return convert(loadV1(xmlText));
  }
  
  public com.terracottatech.config.TcConfigDocument
  convert(String xmlText, XmlOptions xmlOptions) throws XmlException {
    return convert(loadV1(xmlText, xmlOptions));
  }

  public com.terracottatech.config.TcConfigDocument
  convert(InputStream stream) throws IOException, XmlException {
    return convert(loadV1(stream));
  }
  
  public com.terracottatech.config.TcConfigDocument
  convert(InputStream stream, XmlOptions xmlOptions) throws IOException, XmlException {
    return convert(loadV1(stream, xmlOptions));
  }

  public com.terracottatech.config.TcConfigDocument
  convert(URL url) throws IOException, XmlException {
    return convert(loadV1(url));
  }
  
  public com.terracottatech.config.TcConfigDocument
  convert(URL url, XmlOptions xmlOptions) throws IOException, XmlException {
    return convert(loadV1(url, xmlOptions));
  }
  
  private void informTranslated(com.terracottatech.config.TcConfigDocument v2Doc) {
    TCLogger logger = CustomerLogging.getConsoleLogger();
    
    logger.info("Configuration was translated to current version.");
    logger.info("Please update your configuration source to the following:");
    logger.info(v2Doc.xmlText(defaultXmlOptions));
  }
  
  public com.terracottatech.config.TcConfigDocument
  parse(File file) throws IOException, XmlException {
    return parse(file, defaultXmlOptions);
  }
  
  public com.terracottatech.config.TcConfigDocument
  parse(File file, XmlOptions xmlOptions) throws IOException, XmlException {
    com.terracottatech.config.TcConfigDocument v2Doc = null;
  
    try {
      v2Doc = loadV2(file, xmlOptions);
    } catch(XmlException xmle) {
      try {
        v2Doc = convert(file);
        informTranslated(v2Doc);
      } catch(XmlException xmle2) {
        throw xmle;
      }
    }
    
    return v2Doc;
  }
  
  public com.terracottatech.config.TcConfigDocument
  parse(String xmlText) throws XmlException {
    return parse(xmlText, defaultXmlOptions);
  }

  public com.terracottatech.config.TcConfigDocument
  parse(String xmlText, XmlOptions xmlOptions) throws XmlException {
    com.terracottatech.config.TcConfigDocument v2Doc = null;
  
    try {
      v2Doc = loadV2(xmlText, xmlOptions);
    } catch(XmlException xmle) {
      try {
        v2Doc = convert(xmlText, xmlOptions);
        informTranslated(v2Doc);
      } catch(XmlException xmle2) {
        throw xmle;
      }
    }
    
    return v2Doc;
  }

  public com.terracottatech.config.TcConfigDocument
  parse(InputStream stream) throws IOException, XmlException {
    return parse(stream, defaultXmlOptions);
  }
  
  public com.terracottatech.config.TcConfigDocument
  parse(InputStream stream, XmlOptions xmlOptions) throws IOException, XmlException {
    com.terracottatech.config.TcConfigDocument v2Doc = null;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
    CopyUtils.copy(stream, baos);

    try {
      v2Doc = loadV2(new ByteArrayInputStream(baos.toByteArray()), xmlOptions);
    } catch(XmlException xmle) {
      try {
        v2Doc = convert(new ByteArrayInputStream(baos.toByteArray()), xmlOptions);
        informTranslated(v2Doc);
      } catch(XmlException xmle2) {
        throw xmle;
      }
    }
    
    return v2Doc;
  }

  public com.terracottatech.config.TcConfigDocument
  parse(URL url) throws IOException, XmlException {
    return parse(url, defaultXmlOptions);
  }
  
  public com.terracottatech.config.TcConfigDocument
  parse(URL url, XmlOptions xmlOptions) throws IOException, XmlException {
    com.terracottatech.config.TcConfigDocument v2Doc = null;
  
    try {
      v2Doc = loadV2(url, xmlOptions);
    } catch(XmlException xmle) {
      try {
        v2Doc = convert(url, xmlOptions);
        informTranslated(v2Doc);
      } catch(XmlException xmle2) {
        throw xmle;
      }
    }
    
    return v2Doc;
  }

  public com.terracottatech.configV1.TcConfigDocument
  loadV1(File file) throws IOException, XmlException {
    return loadV1(file, defaultXmlOptions);
  }
  
  public com.terracottatech.configV1.TcConfigDocument
  loadV1(File file, XmlOptions xmlOptions) throws IOException, XmlException {
    return com.terracottatech.configV1.TcConfigDocument.Factory.parse(file, xmlOptions);
  }

  public com.terracottatech.configV1.TcConfigDocument
  loadV1(String xmlText) throws XmlException {
    return loadV1(xmlText, defaultXmlOptions);
  }
  
  public com.terracottatech.configV1.TcConfigDocument
  loadV1(String xmlText, XmlOptions xmlOptions) throws XmlException {
    return com.terracottatech.configV1.TcConfigDocument.Factory.parse(xmlText, xmlOptions);
  }

  public com.terracottatech.configV1.TcConfigDocument
  loadV1(InputStream stream) throws IOException, XmlException {
    return loadV1(stream);
  }
  
  public com.terracottatech.configV1.TcConfigDocument
  loadV1(InputStream stream, XmlOptions xmlOptions) throws IOException, XmlException {
    return com.terracottatech.configV1.TcConfigDocument.Factory.parse(stream, xmlOptions);
  }

  public com.terracottatech.configV1.TcConfigDocument
  loadV1(URL url) throws IOException, XmlException {
    return loadV1(url);
  }

  public com.terracottatech.configV1.TcConfigDocument
  loadV1(URL url, XmlOptions xmlOptions) throws IOException, XmlException {
    return com.terracottatech.configV1.TcConfigDocument.Factory.parse(url, xmlOptions);
  }

  public com.terracottatech.config.TcConfigDocument
  loadV2(File file) throws IOException, XmlException {
    return loadV2(file, defaultXmlOptions);
  }

  public com.terracottatech.config.TcConfigDocument
  loadV2(File file, XmlOptions xmlOptions) throws IOException, XmlException {
    return com.terracottatech.config.TcConfigDocument.Factory.parse(file, xmlOptions);
  }

  public com.terracottatech.config.TcConfigDocument
  loadV2(String xmlText) throws XmlException {
    return loadV2(xmlText, defaultXmlOptions);
  }

  public com.terracottatech.config.TcConfigDocument
  loadV2(String xmlText, XmlOptions xmlOptions) throws XmlException {
    return com.terracottatech.config.TcConfigDocument.Factory.parse(xmlText, xmlOptions);
  }

  public com.terracottatech.config.TcConfigDocument
  loadV2(InputStream stream) throws IOException, XmlException {
    return loadV2(stream, defaultXmlOptions);
  }

  public com.terracottatech.config.TcConfigDocument
  loadV2(InputStream stream, XmlOptions xmlOptions) throws IOException, XmlException {
    return com.terracottatech.config.TcConfigDocument.Factory.parse(stream, xmlOptions);
  }

  public com.terracottatech.config.TcConfigDocument
  loadV2(URL url) throws IOException, XmlException {
    return loadV2(url, defaultXmlOptions);
  }

  public com.terracottatech.config.TcConfigDocument
  loadV2(URL url, XmlOptions xmlOptions) throws IOException, XmlException {
    return com.terracottatech.config.TcConfigDocument.Factory.parse(url, xmlOptions);
  }

  public void save(File file,
                   com.terracottatech.config.TcConfigDocument v2Doc)
    throws IOException
  {
    save(file, v2Doc, defaultXmlOptions);
  }
 
  public void save(File file,
                   com.terracottatech.config.TcConfigDocument v2Doc,
                   XmlOptions xmlOptions)
    throws IOException
  {
    v2Doc.save(file, xmlOptions);
  }

  public void save(OutputStream stream,
                   com.terracottatech.config.TcConfigDocument v2Doc)
    throws IOException
  {
    save(stream, v2Doc, defaultXmlOptions);
  }
  
  public void save(OutputStream stream,
                   com.terracottatech.config.TcConfigDocument v2Doc,
                   XmlOptions xmlOptions)
    throws IOException
  {
    v2Doc.save(stream, xmlOptions);
  }
  
  public boolean testIsV2(File file) throws IOException, XmlException {
    try {
      loadV2(file);
      return true;
    } catch(XmlException xmle) {
      return false;
    }
  }
  
  public boolean testIsV2(InputStream stream) throws IOException, XmlException {
    try {
      loadV2(stream);
      return true;
    } catch(XmlException xmle) {
      return false;
    }
  }
  
  public boolean testIsV2(String xmlText) throws XmlException {
    try {
      loadV2(xmlText);
      return true;
    } catch(XmlException xmle) {
      return false;
    }
  }

  public boolean testIsV2(URL url) throws IOException, XmlException {
    try {
      loadV2(url);
      return true;
    } catch(XmlException xmle) {
      return false;
    }
  }
  
  public boolean testIsV1(File file) throws IOException, XmlException {
    try {
      loadV1(file);
      return true;
    } catch(XmlException xmle) {
      return false;
    }
  }
  
  public boolean testIsV1(InputStream stream) throws IOException, XmlException {
    try {
      loadV1(stream);
      return true;
    } catch(XmlException xmle) {
      return false;
    }
  }
  
  public boolean testIsV1(String xmlText) throws XmlException {
    try {
      loadV1(xmlText);
      return true;
    } catch(XmlException xmle) {
      return false;
    }
  }

  public boolean testIsV1(URL url) throws IOException, XmlException {
    try {
      loadV1(url);
      return true;
    } catch(XmlException xmle) {
      return false;
    }
  }

  public void update(File file) throws IOException, XmlException {
    if(testIsV1(file)) {
      save(file, convert(file));
    }
  }
 
  public void update(InputStream inStream, OutputStream outStream)
    throws IOException, XmlException
  {
    if(testIsV1(inStream)) {
      save(outStream, convert(inStream));
    }
  }
  
  protected XmlOptions createDefaultXmlOptions() {
    XmlOptions opts = new XmlOptions();
    
    opts.setLoadLineNumbers();
    opts.setValidateOnSet();
    opts.setSavePrettyPrint();
    opts.setSavePrettyPrintIndent(3);
    opts.remove(XmlOptions.LOAD_STRIP_WHITESPACE);
    opts.remove(XmlOptions.LOAD_STRIP_COMMENTS);
    
    return opts;
  }
  
  public static void main(String[] args) {
    if(args.length > 0) {
      V1toV2 converter = new V1toV2();
      
      for(int i = 0; i < args.length; i++) {
        File file = new File(args[i]);
        
        if(file.exists()) {
          try {
            if(converter.testIsV2(file)) {
              System.out.println("Did not update '"+file.getPath()+"': update unnecessary.");
            } else {
              converter.update(file);
              System.out.println("Updated '"+file.getPath()+"' to current configuration format.");
            }
          } catch(Exception e) {
            String msg = e.getLocalizedMessage();
            System.out.println("Did not update '"+file.getPath()+"': "+msg);
          }
        }
      }
    }
  }
}
