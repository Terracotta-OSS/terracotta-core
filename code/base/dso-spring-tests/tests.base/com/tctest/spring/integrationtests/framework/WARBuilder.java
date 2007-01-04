/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.framework;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.apache.tools.ant.taskdefs.War;
import org.apache.tools.ant.types.ZipFileSet;
import org.codehaus.cargo.container.internal.util.AntUtils;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.remoting.rmi.RmiRegistryFactoryBean;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;

import com.tc.test.TestConfigObject;
import com.tctest.spring.integrationtests.framework.web.RemoteContextListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import junit.framework.Assert;

// TODO THIS SHOULD BE REWRITTEN TO USE Velocity as the templating engine.

public class WARBuilder implements DeploymentBuilder {

  Log                    logger              = LogFactory.getLog(getClass());

  private FileSystemPath warDirectoryPath;

  private String         warFileName;

  private Set            classDirectories    = new HashSet(); /* <FileSystemPath> */

  private Set            libs                = new HashSet();
  
  private List           resources           = new ArrayList();

  private List           remoteServices      = new ArrayList();

  private Set            beanDefinitionFiles = new HashSet();

  private Map            contextParams       = new HashMap();

  private List           listeners           = new ArrayList();

  private List           servlets           = new ArrayList();

  private Map            taglibs             = new HashMap();

  private StringBuffer   remoteSvcDefBlock   = new StringBuffer();

  private final FileSystemPath tempDirPath;
  
  private String         dispatcherServletName = null;
  
  private final TestConfigObject testConfig;


  public WARBuilder(File tempDir, TestConfigObject config) throws IOException {
    this(File.createTempFile("test", ".war", tempDir).getAbsolutePath(), tempDir, config);
  }

  public WARBuilder(String warFileName, File tempDir, TestConfigObject config) {
    this.warFileName = warFileName;
    this.tempDirPath = new FileSystemPath(tempDir);
    this.testConfig = config;

    addDirectoryOrJARContainingClass(RemoteContextListener.class);  // test framework
    addDirectoryOrJARContainingClass(LogFactory.class);  // commons-logging
    addDirectoryOrJARContainingClass(Logger.class);  // log4j

    // XXX this should NOT be in such general purpose class!
    // XXX this should add ALL jars from the variant directory!
    addDirectoryOrJARContainingClassOfSelectedVersion(BeanFactory.class, new String[]{TestConfigObject.SPRING_VARIANT});  // springframework
    // addDirectoryOrJARContainingClass(BeanFactory.class);  // springframework
  }

  public DeploymentBuilder addClassesDirectory(FileSystemPath path) {
    classDirectories.add(path);
    return this;
  }

  public Deployment makeDeployment() throws Exception {
    createWARDirectory();

    FileSystemPath warFile = makeWARFileName();
    logger.debug("Creating war file: " + warFile);
    warFile.delete();

    War warTask = makeWarTask();
    warTask.setUpdate(false);
    warTask.setDestFile(warFile.getFile());
    warTask.setWebxml(warDirectoryPath.existingFile("WEB-INF/web.xml").getFile());
    addWEBINFDirectory(warTask);
    addClassesDirectories(warTask);
    addLibs(warTask);
    addResources(warTask);
    warTask.execute();

    return new WARDeployment(warFile);
  }

  private FileSystemPath makeWARFileName() {
    File f = new File(warFileName);
    if (f.isAbsolute()) {
      return FileSystemPath.makeNewFile(warFileName);
    } else {
      return tempDirPath.file(warFileName);
    }
  }

  private void addLibs(War warTask) {
    for (Iterator it = libs.iterator(); it.hasNext();) {
      FileSystemPath lib = (FileSystemPath) it.next();
      ZipFileSet zipFileSet = new ZipFileSet();
      zipFileSet.setFile(lib.getFile());
      warTask.addLib(zipFileSet);
    }
  }

  private War makeWarTask() {
    return (War) new AntUtils().createAntTask("war");
  }

  private void addClassesDirectories(War warTask) {
    for (Iterator it = classDirectories.iterator(); it.hasNext();) {
      FileSystemPath path = (FileSystemPath) it.next();
      ZipFileSet zipFileSet = new ZipFileSet();
      zipFileSet.setDir(path.getFile());
      warTask.addClasses(zipFileSet);
    }
  }
  
  private void addResources(War warTask) {
    for(Iterator it = resources.iterator(); it.hasNext(); ) {
      ResourceDefinition definition = (ResourceDefinition) it.next();
      ZipFileSet zipfileset = new ZipFileSet();
      zipfileset.setDir(definition.location);
      zipfileset.setIncludes(definition.includes);
      zipfileset.setPrefix(definition.prefix);
      warTask.addZipfileset(zipfileset);
    }
  }

  private void addWEBINFDirectory(War warTask) {
    ZipFileSet zipFileSet = new ZipFileSet();
    zipFileSet.setDir(warDirectoryPath.getFile());
    warTask.addFileset(zipFileSet);
  }

  public DeploymentBuilder addClassesDirectory(String directory) {
    return addClassesDirectory(FileSystemPath.existingDir(directory));
  }

  void createWARDirectory() throws IOException {
    this.warDirectoryPath = tempDirPath.mkdir("tempwar");
    FileSystemPath webInfDir = warDirectoryPath.mkdir("WEB-INF");
    createWebXML(webInfDir);
    if (this.dispatcherServletName == null) {
      createRemotingContext(webInfDir);
    } else {
      createDispatcherServletContext(webInfDir);
    }
  }

  private void createDispatcherServletContext(FileSystemPath webInfDir) throws IOException {
    FileSystemPath springRemotingAppCtx = webInfDir.file(this.dispatcherServletName + "-servlet.xml");
    FileOutputStream fos = new FileOutputStream(springRemotingAppCtx.getFile());

    try {
      appendFile(fos, "/dispatcherServletContextHeader.txt");
      PrintWriter pw = new PrintWriter(fos);
      pw.println(remoteSvcDefBlock.toString());

      writeHandlerMappingBean(pw);

      for (Iterator it = remoteServices.iterator(); it.hasNext();) {
        RemoteService remoteService = (RemoteService) it.next();
        writeRemoteService(pw, remoteService);
      }
      pw.flush();
      writeFooter(fos);
    } finally {
      fos.close();
    }
  }

  private void createRemotingContext(FileSystemPath webInfDir) throws IOException {
    FileSystemPath springRemotingAppCtx = webInfDir.mkdir("classes/com/tctest/spring").file("spring-remoting.xml");
    FileOutputStream fos = new FileOutputStream(springRemotingAppCtx.getFile());

    try {
      appendFile(fos, "/remoteContextHeader.txt");
      PrintWriter pw = new PrintWriter(fos);
      pw.println(remoteSvcDefBlock.toString());

      writeRegistryFactoryBean(pw);

      for (Iterator it = remoteServices.iterator(); it.hasNext();) {
        RemoteService remoteService = (RemoteService) it.next();
        writeRemoteService(pw, remoteService);
      }
      pw.flush();
      writeFooter(fos);
    } finally {
      fos.close();
    }
  }
  
  private void writeHandlerMappingBean(PrintWriter pw) {
    pw.println("<bean id=\"defaultHandlerMapping\" class=\"" + BeanNameUrlHandlerMapping.class.getName() + "\"/>");
  }

  private void writeRegistryFactoryBean(PrintWriter pw) {
    pw.println("<bean class=\"" + RmiRegistryFactoryBean.class.getName() + "\"  name=\"registry\" >");
    pw.println("<property name=\"port\" value=\"${rmi.registry.port}\" />");
    pw.println("</bean>");
  }

  private void writeRemoteService(PrintWriter pw, RemoteService remoteService) {
    if (this.dispatcherServletName == null) {
      pw.println("<bean class=\"" + remoteService.getExporterType().getName() + "\">");
      printProperty(pw, "serviceName", remoteService.getRemoteName());
      printPropertyRef(pw, "service", remoteService.getBeanName());
      printProperty(pw, "serviceInterface", remoteService.getInterfaceType().getName());
      printPropertyRef(pw, "registry", "registry");
      pw.println("</bean>");
    } else {
      pw.println("<bean name=\"/" + remoteService.getRemoteName() +  "\" class=\"" + remoteService.getExporterType().getName() + "\">");
      printPropertyRef(pw, "service", remoteService.getBeanName());
      printProperty(pw, "serviceInterface", remoteService.getInterfaceType().getName());
      pw.println("</bean>");      
    }
  }

  private void printProperty(PrintWriter pw, String propertyName, String propertyValue) {
    pw.println("<property name=\"" + propertyName + "\" value=\"" + propertyValue + "\" />");
  }

  private void printPropertyRef(PrintWriter pw, String propertyName, String propertyValue) {
    pw.println("<property name=\"" + propertyName + "\" ref=\"" + propertyValue + "\" />");
  }

  private void writeFooter(FileOutputStream fos) throws IOException {
    appendFile(fos, "/remoteContextFooter.txt");
  }

  private void appendFile(FileOutputStream fos, String fragmentName) throws IOException {
    InputStream is = getClass().getResourceAsStream(fragmentName);
    try {
      // Yes. This needs to be optimized
      int i;
      while ((i = is.read()) != -1)
        fos.write(i);
    } finally {
      is.close();
    }

  }

  private void createWebXML(FileSystemPath webInfDir) throws IOException {
    FileSystemPath webXML = webInfDir.file("web.xml");
    FileOutputStream fos = new FileOutputStream(webXML.getFile());
    try {
      PrintWriter pw = new PrintWriter(fos);
      
      
      pw.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");

//      if (testConfig.appserverFactoryName().indexOf("weblogic8")>=0) {
        pw.println("<!DOCTYPE web-app PUBLIC \"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN\" \"http://java.sun.com/dtd/web-app_2_3.dtd\">"); 
        pw.println("<web-app>\n");
//      } else {
//        pw.println("<web-app xmlns=\"http://java.sun.com/xml/ns/j2ee\"\n" + 
//            "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
//            "    xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd\"\n" + 
//            "    version=\"2.4\">");
//      }
      
      if(!beanDefinitionFiles.isEmpty()) {
        writeContextParam(pw, ContextLoader.CONFIG_LOCATION_PARAM, generateContextConfigLocationValue());
      }
      
      for (Iterator it = contextParams.entrySet().iterator(); it.hasNext();) {
        Map.Entry param = (Map.Entry) it.next();
        writeContextParam(pw, (String) param.getKey(), (String) param.getValue());
      }
      
      if(!beanDefinitionFiles.isEmpty()) {
        writeListener(pw, org.springframework.web.context.ContextLoaderListener.class.getName());
        if (this.dispatcherServletName == null) {
          writeListener(pw, com.tctest.spring.integrationtests.framework.web.RemoteContextListener.class.getName());
        }
      }
      for (Iterator it = listeners.iterator(); it.hasNext();) {
        writeListener(pw, ((Class) it.next()).getName());
      }
      
      for (Iterator it = servlets.iterator(); it.hasNext();) {
        ServletDefinition definition = (ServletDefinition) it.next();
        writeServlet(pw, definition);
      }
      for (Iterator it = servlets.iterator(); it.hasNext();) {
        ServletDefinition definition = (ServletDefinition) it.next();
        pw.println("  <servlet-mapping>");
        pw.println("    <servlet-name>"+definition.name+"</servlet-name>");
        pw.println("    <url-pattern>"+definition.mapping+"</url-pattern>");
        pw.println("  </servlet-mapping>");
      }
      
      if(!taglibs.isEmpty()) {
        pw.println("  <jsp-config>");
        for (Iterator it = taglibs.entrySet().iterator(); it.hasNext();) {
          Map.Entry taglib = (Map.Entry) it.next();
          pw.println("    <taglib-uri>"+taglib.getKey()+"</taglib-uri>");
          pw.println("    <taglib-location>"+taglib.getValue()+"</taglib-location>");
        }
        pw.println("  </jsp-config>");
      }
      
      pw.println("</web-app>");
      pw.flush();

    } finally {
      fos.close();
    }
  }

  private void writeContextParam(PrintWriter pw, String name, String value) {
    pw.println("  <context-param>");
    pw.println("    <param-name>"+name+"</param-name>");
    pw.println("    <param-value>"+value+"</param-value>");
    pw.println("  </context-param>");
  }

  private void writeListener(PrintWriter pw, String className) {
    pw.println("  <listener>");
    pw.println("    <listener-class>"+className+"</listener-class>");
    pw.println("  </listener>");
  }

  private void writeServlet(PrintWriter pw, ServletDefinition definition) {
    pw.println("  <servlet>");
    pw.println("    <servlet-name>"+definition.name+"</servlet-name>");
    pw.println("    <servlet-class>"+definition.servletClass.getName()+"</servlet-class>");

    if(definition.initParameters!=null) {
      for (Iterator it = definition.initParameters.entrySet().iterator(); it.hasNext();) {
        Map.Entry param = (Map.Entry) it.next();
        pw.println("    <init-param>");
        pw.println("      <param-name>"+param.getKey()+"</param-name>");
        pw.println("      <param-value>"+param.getValue()+"</param-value>");
        pw.println("    </init-param>");
      }
    }
    
    if(definition.loadOnStartup) {
      pw.println("    <load-on-startup>1</load-on-startup>");
    }
    
    pw.println("  </servlet>");
  }
  
  private String generateContextConfigLocationValue() {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    for (Iterator it = beanDefinitionFiles.iterator(); it.hasNext();) {
      String beanDefinitionFile = (String) it.next();
      pw.println(beanDefinitionFile);
    }
    pw.flush();
    return sw.toString();
  }

  public DeploymentBuilder addBeanDefinitionFile(String beanDefinition) {
    beanDefinitionFiles.add(beanDefinition);
    return this;
  }

  public DeploymentBuilder addRemoteService(String remoteName, String beanName, Class interfaceType) {
    remoteServices.add(new RemoteService(remoteName, beanName, interfaceType));
    return this;
  }

  public DeploymentBuilder addRemoteService(Class exporterType, String remoteName, String beanName, Class interfaceType) {
    remoteServices.add(new RemoteService(exporterType, remoteName, beanName, interfaceType));
    return this;
  }

  public DeploymentBuilder addRemoteService(String beanName, Class interfaceType) {
    addRemoteService(StringUtils.capitaliseAllWords(beanName), beanName, interfaceType);
    return this;
  }

  public DeploymentBuilder addRemoteServiceBlock(String block) {
    remoteSvcDefBlock.append(block + "\n");
    return this;
  }

  public void setParentApplicationContextRef(String locatorFactorySelector, String parentContextKey) {
    this.contextParams.put(ContextLoader.LOCATOR_FACTORY_SELECTOR_PARAM, locatorFactorySelector);
    this.contextParams.put(ContextLoader.LOCATOR_FACTORY_KEY_PARAM, parentContextKey);
  }

  public DeploymentBuilder addDirectoryOrJARContainingClass(Class type) {
    return addDirectoryOrJar(calculatePathToClass(type));
  }
  
  public DeploymentBuilder addDirectoryOrJARContainingClassOfSelectedVersion(Class type, String[] variantNames) {
    String pathSeparator = System.getProperty("path.separator");
    
    for (int i = 0; i < variantNames.length; i++) {
      String selectedVariant = testConfig.selectedVariantFor(variantNames[i]);
      String path = testConfig.variantLibraryClasspathFor(variantNames[i], selectedVariant);
      String[] paths = path.split(pathSeparator);
      for (int j = 0; j < paths.length; j++) {
        addDirectoryOrJar(new FileSystemPath(new File(paths[j])));
      }
    }

    return this;
  }
  
  public DeploymentBuilder addDirectoryContainingResource(String resource) {
    return addDirectoryOrJar(calculatePathToResource(resource));
  }

  public DeploymentBuilder addResource(String location, String includes, String prefix) {
    String resource = location + "/" + includes;
    URL url = getClass().getResource(resource);
    Assert.assertNotNull("Not found: " + resource, url);
    FileSystemPath path = calculateDirectory(url, includes);
    resources.add(new ResourceDefinition(path.getFile(), includes, prefix));
    return this;
  }
  
  public DeploymentBuilder addContextParameter(String name, String value) {
    contextParams.put(name, value);
    return this;
  }
  
  public DeploymentBuilder addListener(Class listenerClass) {
    listeners.add(listenerClass);
    return this;
  }
  
  public DeploymentBuilder setDispatcherServlet(String name, String mapping, Class servletClass, Map params, boolean loadOnStartup) {
    Assert.assertNull(this.dispatcherServletName);
    this.dispatcherServletName = name;
    addServlet(name, mapping, servletClass, params, loadOnStartup);
    return this;
  }
  
  public DeploymentBuilder addServlet(String name, String mapping, Class servletClass, Map params, boolean loadOnStartup) {
    servlets.add(new ServletDefinition(name, mapping, servletClass, params, loadOnStartup));
    return this;
  }
  
  public DeploymentBuilder addTaglib(String uri, String location) {
    taglibs.put(uri, location);
    return this;
  }
  
  private DeploymentBuilder addDirectoryOrJar(FileSystemPath path) {
    if (path.isDirectory()) {
      classDirectories.add(path);
    } else {
      libs.add(path);
    }
    return this;
  }
  
  public static FileSystemPath calculatePathToClass(Class type) {
    URL url = type.getResource("/" + classToPath(type));
    Assert.assertNotNull("Not found: " + type, url);
    FileSystemPath filepath = calculateDirectory(url, "/" + classToPath(type));
    return filepath;
  }
  
  static public FileSystemPath calculatePathToClass(Class type, String pathString) {
    String pathSeparator = System.getProperty("path.separator");
    StringTokenizer st = new StringTokenizer(pathString, pathSeparator);
    URL[] urls = new URL[st.countTokens()];
    for (int i = 0; st.hasMoreTokens(); i++) {
      String token = st.nextToken();
      if (token.startsWith("/")) {
        token = "/" + token;
      }
      URL u = null;
      try {
        if (token.endsWith(".jar")) {
          u = new URL("jar", "", "file:/" + token + "!/");
        } else {
          u = new URL("file", "", token + "/");
        }
        urls[i] = u;
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }
    URL url = new URLClassLoader(urls, null).getResource(classToPath(type));
    Assert.assertNotNull("Not found: " + type, url);
    FileSystemPath filepath = calculateDirectory(url, "/" + classToPath(type));
    return filepath;
  }

  public static FileSystemPath calculateDirectory(URL url, String classNameAsPath) {
    
    String urlAsString = null;
    try {
      urlAsString = java.net.URLDecoder.decode(url.toString(), "UTF-8");
    } catch (Exception ex ) { throw new RuntimeException(ex); } 
    Assert.assertTrue("URL should end with: " + classNameAsPath, urlAsString.endsWith(classNameAsPath));
    if (urlAsString.startsWith("file:")) {
      return FileSystemPath.existingDir(urlAsString.substring("file:".length(), urlAsString.length()
          - classNameAsPath.length()));
    } else if (urlAsString.startsWith("jar:file:")) {
      int n = urlAsString.indexOf('!');
      return FileSystemPath.makeExistingFile(urlAsString.substring("jar:file:".length(), n ));
    } else throw new RuntimeException("unsupported protocol: " + url);
  }

  private static String classToPath(Class type) {
    return type.getName().replace('.', '/') + ".class";
  }

  private FileSystemPath calculatePathToResource(String resource) {
    URL url = getClass().getResource(resource);
    Assert.assertNotNull("Not found: " + resource, url);
    return calculateDirectory(url, resource);
  }

  
  private static class ResourceDefinition {
    public final File location;
    public final String prefix;
    public final String includes;

    public ResourceDefinition(File location, String includes, String prefix) {
      this.location = location;
      this.includes = includes;
      this.prefix = prefix;
    }
  }
  
  
  private static class ServletDefinition {
    public final String name;
    public final String mapping;
    public final Class servletClass;
    public final Map initParameters;
    public final boolean loadOnStartup;
    
    public ServletDefinition(String name, String mapping, Class servletClass, Map initParameters, boolean loadOnStartup) {
      this.name = name;
      this.mapping = mapping;
      this.servletClass = servletClass;
      this.initParameters = initParameters;
      this.loadOnStartup = loadOnStartup;
    }
  }
  
}
