/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.StandardClasspathProvider;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * ClasspathProvider for Terracotta processes, such as TCServer and BootJarTool. Not used for DSO clients.
 */
public class ClasspathProvider extends StandardClasspathProvider {
  public ClasspathProvider() {
    super();
  }

  @Override
  public IRuntimeClasspathEntry[] computeUnresolvedClasspath(ILaunchConfiguration configuration) {
    IPath jarPath = TcPlugin.getDefault().getLibDirPath().append("tc.jar");

    if (jarPath.toFile().exists()) {
      return new IRuntimeClasspathEntry[] {
          JavaRuntime.newArchiveRuntimeClasspathEntry(jarPath.removeLastSegments(1).append("resources")),
          JavaRuntime.newArchiveRuntimeClasspathEntry(jarPath.makeAbsolute()) };
    } else {
      ArrayList<IRuntimeClasspathEntry> list = new ArrayList<IRuntimeClasspathEntry>();
      IPath[] paths = gatherDevClasspathEntries();

      for (IPath path : paths) {
        list.add(JavaRuntime.newArchiveRuntimeClasspathEntry(path));
      }

      return list.toArray(new IRuntimeClasspathEntry[0]);
    }
  }

  public static String makeDevClasspath() {
    IPath[] paths = gatherDevClasspathEntries();
    StringBuffer sb = new StringBuffer();
    String sep = System.getProperty("path.separator");

    for (int i = 0; i < paths.length; i++) {
      if (i > 0) {
        sb.append(sep);
      }
      sb.append(paths[i].toOSString());
    }

    return sb.toString();
  }

  private static Collection<File> listArchives(File dir) {
    Collection c = FileUtils.listFiles(dir, new String[] { "jar" }, false);
    Collection<File> result = new HashSet<File>();
    Iterator iter = c.iterator();

    while (iter.hasNext()) {
      result.add((File) iter.next());
    }

    return result;
  }

  private static IPath[] gatherDevClasspathEntries() {
    IPath location = TcPlugin.getDefault().getLocation();
    List<IPath> list = new ArrayList<IPath>();
    IPath buildPath = location.append("..");

    String[] dirs = { "deploy", "deploy-api", "common", "common-api", "management", "management-api", "aspectwerkz",
        "sigarstats", "thirdparty", "dso-common", "dso-common-jdk16", "dso-l1", "dso-l1-api", "dso-l2",
        "dso-l2-common", "dso-statistics", "dso-statistics-api", "tim-get-tool", "tim-api" };

    for (String dir : dirs) {
      IPath classesPath = buildPath.append(dir).append("build.eclipse").append("src.classes");
      if (classesPath.toFile().exists()) {
        list.add(classesPath);
      }
    }

    // this is to get access to build-data.txt in dev mode
    list.add(buildPath.append("common").append("build.eclipse").append("tests.base.classes"));

    final List<File> fileList = new ArrayList<File>();
    File libDir;

    for (String dir : dirs) {
      libDir = location.append("..").append(dir).append("lib").toFile();
      if (libDir.exists()) {
        fileList.addAll(listArchives(libDir));
      }
    }

    final File dependencies = location.append("..").append("dependencies").append("lib").toFile();
    File ivy = null;
    File projectDir = null;
    String dir = null;
    try {
      for (String dir2 : dirs) {
        dir = dir2;
        projectDir = location.append("..").append(dir).toFile();
        ivy = new File(projectDir + File.separator + "ivy.xml");
        if (ivy.exists()) {
          SAXParserFactory factory = SAXParserFactory.newInstance();
          SAXParser parser = factory.newSAXParser();
          parser.parse(new FileInputStream(ivy), new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) {
              if (qName.equals("dependency")) {
                String jar = attributes.getValue("name") + "-" + attributes.getValue("rev");
                File jarFile = new File(dependencies + File.separator + jar + ".jar");
                fileList.add(jarFile);
              }
            }
          });
        }
      }
    } catch (Throwable e) {
      TcPlugin.getDefault().openError("Problem Parsing ivy.xml file in: " + dir, e);
    }

    Iterator fileIter = fileList.iterator();
    File file;

    while (fileIter.hasNext()) {
      file = (File) fileIter.next();
      if (!file.getName().startsWith("org.eclipse")) {
        list.add(new Path(file.getAbsolutePath()));
      }
    }

    return list.toArray(new IPath[list.size()]);
  }
}
