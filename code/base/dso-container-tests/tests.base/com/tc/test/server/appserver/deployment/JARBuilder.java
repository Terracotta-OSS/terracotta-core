/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.deployment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.taskdefs.Zip.Duplicate;
import org.apache.tools.ant.types.ZipFileSet;
import org.codehaus.cargo.util.AntUtils;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

public class JARBuilder {

  private static final Log     logger           = LogFactory.getLog(JARBuilder.class);

  private FileSystemPath       jarDirectoryPath;
  private String               jarFileName;
  private Set                  classDirectories = new HashSet();
  private Set                  libs             = new HashSet();
  private List                 resources        = new ArrayList();
  private final FileSystemPath tempDirPath;

  public JARBuilder(String warFileName, File tempDir) {
    this.jarFileName = warFileName;
    this.tempDirPath = new FileSystemPath(tempDir);

  }

  public JARBuilder addClassesDirectory(FileSystemPath path) {
    classDirectories.add(path);
    return this;
  }

  public void finish() throws Exception {
    FileSystemPath jarFile = makejarFileName();
    logger.debug("Creating jar file: " + jarFile);
    jarFile.delete();

    Jar jarTask = (Jar) new AntUtils().createAntTask("jar");
    jarTask.setUpdate(false);

    Duplicate df = new Duplicate();
    df.setValue("preserve");
    jarTask.setDuplicate(df);
    jarTask.setDestFile(jarFile.getFile());

    addClassesDirectories(jarTask);
    addLibs(jarTask);
    addResources(jarTask);
    jarTask.execute();
  }

  private FileSystemPath makejarFileName() {
    File f = new File(jarFileName);
    if (f.isAbsolute()) {
      return FileSystemPath.makeNewFile(jarFileName);
    } else {
      return tempDirPath.file(jarFileName);
    }
  }

  private void addLibs(Jar jarTask) {
    for (Iterator it = libs.iterator(); it.hasNext();) {
      FileSystemPath lib = (FileSystemPath) it.next();
      ZipFileSet zipFileSet = new ZipFileSet();
      zipFileSet.setFile(lib.getFile());
      jarTask.addZipfileset(zipFileSet);
    }
  }

  private void addClassesDirectories(Jar jarTask) {
    for (Iterator it = classDirectories.iterator(); it.hasNext();) {
      FileSystemPath path = (FileSystemPath) it.next();
      ZipFileSet zipFileSet = new ZipFileSet();
      zipFileSet.setDir(path.getFile());
      jarTask.addZipfileset(zipFileSet);
    }
  }

  private void addResources(Jar jarTask) {
    for (Iterator it = resources.iterator(); it.hasNext();) {
      ResourceDefinition definition = (ResourceDefinition) it.next();
      ZipFileSet zipfileset = new ZipFileSet();
      zipfileset.setDir(definition.location);
      zipfileset.setIncludes(definition.includes);
      if (definition.prefix != null) zipfileset.setPrefix(definition.prefix);
      if (definition.fullpath != null) zipfileset.setFullpath(definition.fullpath);
      jarTask.addZipfileset(zipfileset);
    }
  }

  public JARBuilder addClassesDirectory(String directory) {
    return addClassesDirectory(FileSystemPath.existingDir(directory));
  }

  void createJarDirectory() {
    this.jarDirectoryPath = tempDirPath.mkdir("tempjar");
    jarDirectoryPath.mkdir("META-INF");
  }

  public JARBuilder addDirectoryOrJARContainingClass(Class type) {
    return addDirectoryOrJar(calculatePathToClass(type));
  }

  public JARBuilder addDirectoryContainingResource(String resource) {
    return addDirectoryOrJar(calculatePathToResource(resource));
  }

  public JARBuilder addResource(String location, String includes, String prefix) {
    FileSystemPath path = getResourceDirPath(location, includes);
    resources.add(new ResourceDefinition(path.getFile(), includes, prefix, null));
    return this;
  }

  public JARBuilder addResourceFullpath(String location, String includes, String fullpath) {
    FileSystemPath path = getResourceDirPath(location, includes);
    resources.add(new ResourceDefinition(path.getFile(), includes, null, fullpath));
    return this;
  }

  private FileSystemPath getResourceDirPath(String location, String includes) {
    String resource = location + "/" + includes;
    URL url = getClass().getResource(resource);
    Assert.assertNotNull("Not found: " + resource, url);
    FileSystemPath path = calculateDirectory(url, includes);
    return path;
  }

  private JARBuilder addDirectoryOrJar(FileSystemPath path) {
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
    String[] tokens = pathString.split(pathSeparator);
    URL[] urls = new URL[tokens.length];
    for (int i = 0; i < tokens.length; i++) {
      String token = tokens[i];
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
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    Assert.assertTrue("URL should end with: " + classNameAsPath, urlAsString.endsWith(classNameAsPath));
    if (urlAsString.startsWith("file:")) {
      return FileSystemPath.existingDir(urlAsString.substring("file:".length(), urlAsString.length()
                                                                                - classNameAsPath.length()));
    } else if (urlAsString.startsWith("jar:file:")) {
      int n = urlAsString.indexOf('!');
      return FileSystemPath.makeExistingFile(urlAsString.substring("jar:file:".length(), n));
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
    public final File   location;
    public final String prefix;
    public final String includes;
    public final String fullpath;

    public ResourceDefinition(File location, String includes, String prefix, String fullpath) {
      this.location = location;
      this.includes = includes;
      this.prefix = prefix;
      this.fullpath = fullpath;
    }
  }
}
