/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.war;

import org.apache.commons.io.FileUtils;

import com.tc.test.server.appserver.unit.TCServletFilterHolder;
import com.tc.util.Assert;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

/**
 * This class knows the contents and structure of a J2EE 2.0 compliant WAR (Web Application Resource). It assembles
 * artifacts such as XML descriptors, libraries, classfiles, and servlets into an uncompressed JAR (as the spec
 * requires). The web.xml file is updated to reflect the servlet mappings of this WAR. See implementations:
 * {@link WebXxml} for details.
 */
public abstract class AbstractWar implements War {

  protected final String      appName;
  private final WebXml        webXml;
  private final List          containerXml;
  private final List          servlets;
  private final List          listeners;
  private final List          filters;
  private final List          libraries;
  private final List          classes;
  private final CRC32         crc32;
  private final HashSet       dirSet   = new HashSet();
  private final HashSet       classSet = new HashSet();

  private static final String WEB_INF  = "WEB-INF/";
  private static final String CLASSES  = "WEB-INF/classes/";
  private static final String LIB      = "WEB-INF/lib/";

  public AbstractWar(String appName, WebXml webXml) {
    Assert.assertNotBlank(appName);
    this.appName = appName;
    this.webXml = webXml;
    this.containerXml = new ArrayList();
    this.servlets = new ArrayList();
    this.listeners = new ArrayList();
    this.filters = new ArrayList();
    this.libraries = new ArrayList();
    this.classes = new ArrayList();
    this.crc32 = new CRC32();
  }

  public final String addServlet(Class servletClass) {
    Assert.assertNotNull(servletClass);
    servlets.add(servletClass);
    return webXml.addServlet(servletClass);
  }

  public final void addListener(Class listenerClass) {
    Assert.assertNotNull(listenerClass);
    listeners.add(listenerClass);
    webXml.addListener(listenerClass);
  }

  public final void addFilter(Class filterClass, String pattern, Map initParams) {
    Assert.assertNotNull(filterClass);
    if (filters.size() == 0) addClass(TCServletFilterHolder.class);
    filters.add(filterClass);
    webXml.addFilter(filterClass, pattern, initParams);
  }

  public final void addClass(Class clazz) {
    Assert.assertNotNull(clazz);
    classes.add(clazz);
  }

  public final void addContainerSpecificXml(String fileName, byte[] containerXmlBytes) {
    containerXml.add(new ContainerXmlValues(fileName, containerXmlBytes));
  }

  private class ContainerXmlValues {
    private String fileName;
    private byte[] bytes;

    private ContainerXmlValues(String fileName, byte[] bytes) {
      this.bytes = bytes;
      this.fileName = fileName;
    }
  }

  public final String writeWarFileToDirectory(File directory) throws IOException {
    Assert.assertNotNull(directory);
    File warFile = new File(directory + File.separator + appName + ".war");
    JarOutputStream jout = null;
    IOException exception = null;
    try {
      jout = new JarOutputStream(new FileOutputStream(warFile), new Manifest());
      jout.setMethod(ZipEntry.STORED);
      jout.setLevel(ZipEntry.STORED);

      putEntry(jout, WEB_INF);
      putEntry(jout, CLASSES);
      putEntry(jout, LIB);
      putEntry(jout, WEB_INF + webXml.getFileName(), webXml.getBytes());
      putClasses(jout, servlets);
      putClasses(jout, listeners);
      putClasses(jout, filters);
      putClasses(jout, classes);
      putLibraries(jout);
      putContainerSpecificXmlFiles(jout);

    } catch (IOException ioe) {
      exception = ioe;
    } finally {
      try {
        if (jout != null) jout.close();
      } catch (IOException ioe2) {
        // ignore
      }
    }
    if (exception != null) {
      FileUtils.cleanDirectory(directory);
      exception.printStackTrace();
      throw new IOException("Unable to create war file.");
    }

    return appName + ".war";
  }

  public final void addLibrary(File lib) {
    Assert.assertNotNull(lib);
    if (!lib.isDirectory() && !lib.getName().endsWith("jar")) { throw new RuntimeException(
                                                                                           "Library must be either a directory or a .jar file"); }
    libraries.add(lib);
  }

  private void putClasses(JarOutputStream jout, List classList) throws IOException {
    Class clazz;
    String[] parts;
    String currentPath;
    String className;

    for (Iterator iter = classList.iterator(); iter.hasNext();) {
      clazz = (Class) iter.next();
      parts = clazz.getName().split("\\.");
      className = parts[parts.length - 1];
      currentPath = CLASSES;

      for (int i = 0; i < (parts.length - 1); i++) {
        currentPath += parts[i] + File.separator;
        if (!dirSet.contains(currentPath)) {
          putEntry(jout, currentPath);
          dirSet.add(currentPath);
        }
      }

      String[] outerClass = className.split("\\$");
      URL servletURL = clazz.getResource(className + ".class");
      String[] files = new File(servletURL.getPath()).getParentFile().list();
      for (int i = 0; i < files.length; i++) {
        if (Pattern.matches(outerClass[0] + "(\\$.*)?\\.class", files[i])) {
          if (!classSet.contains(files[i])) {
            InputStream in = clazz.getResourceAsStream(files[i]);
            byte[] servletBytes = new byte[in.available()];
            in.read(servletBytes);
            putEntry(jout, currentPath + files[i], servletBytes);
            classSet.add(files[i]);
          }
        }
      }
    }
  }

  private void putContainerSpecificXmlFiles(JarOutputStream jout) throws IOException {
    ContainerXmlValues values;
    for (Iterator iter = containerXml.iterator(); iter.hasNext();) {
      values = (ContainerXmlValues) iter.next();
      putEntry(jout, WEB_INF + values.fileName, values.bytes);
    }
  }

  private void putLibraries(JarOutputStream jout) throws IOException {
    File lib;
    for (Iterator iter = libraries.iterator(); iter.hasNext();) {
      lib = (File) iter.next();
      if (lib.getName().endsWith("jar")) {
        InputStream in = new FileInputStream(lib);
        byte[] bytes = new byte[in.available()];
        in.read(bytes);
        putEntry(jout, LIB + lib.getName(), bytes);

      } else {
        createLibraryJar(jout, lib);
      }
    }
  }

  private void createLibraryJar(JarOutputStream jout, File lib) throws IOException {
    List relativePath = new ArrayList();
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    JarOutputStream newj = new JarOutputStream(bout, new Manifest());
    newj.setMethod(ZipEntry.STORED);
    newj.setLevel(ZipEntry.STORED);

    String[] contents = lib.list();
    File dir;
    for (int i = 0; i < contents.length; i++) {
      dir = new File(lib.getPath() + File.separator + contents[i]);
      recurseLibTree(newj, dir, relativePath);
    }
    if (newj != null) newj.close();
    putEntry(jout, LIB + lib.getName() + ".jar", bout.toByteArray());
  }

  private void recurseLibTree(JarOutputStream newj, File file, List relativePath) throws IOException {
    if (file.isDirectory()) {
      putEntry(newj, makePath(relativePath, file.getName()) + "/");
      String[] contents = file.list();
      if (contents.length > 0) relativePath.add(file.getName());
      for (int i = 0; i < contents.length; i++) {
        recurseLibTree(newj, new File(file.getPath() + File.separator + contents[i]), relativePath);
      }
      relativePath.remove(relativePath.size() - 1);

    } else {
      InputStream in = new FileInputStream(file);
      byte[] bytes = new byte[in.available()];
      in.read(bytes);
      putEntry(newj, makePath(relativePath, file.getName()), bytes);
    }
  }

  private String makePath(List path, String file) {
    String url = "";
    for (Iterator iter = path.iterator(); iter.hasNext();) {
      url += (String) iter.next() + "/";
    }
    if (url.equals("")) return file;
    return url + file;
  }

  private void putEntry(JarOutputStream jout, String file) throws IOException {
    ZipEntry entry = new ZipEntry(file + "/");
    entry.setSize(0);
    entry.setCrc(0);
    jout.putNextEntry(entry);
  }

  private void putEntry(JarOutputStream jout, String file, byte[] bytes) throws IOException {
    ZipEntry entry = new ZipEntry(file);
    entry.setSize(bytes.length);
    entry.setCrc(getCrc32(bytes));
    jout.putNextEntry(entry);
    jout.write(bytes, 0, bytes.length);
  }

  private long getCrc32(byte[] bytes) {
    crc32.update(bytes);
    long checksum = crc32.getValue();
    crc32.reset();
    return checksum;
  }
}