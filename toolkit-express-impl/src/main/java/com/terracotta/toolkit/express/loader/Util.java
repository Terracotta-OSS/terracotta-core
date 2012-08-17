/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express.loader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Util {
  public static boolean jboss6x;

  static {
    jboss6x = isJBoss6x();
  }

  public static byte[] extract(final InputStream in) throws IOException {
    if (in == null) { throw new NullPointerException(); }

    try {
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      final byte[] data = new byte[4096];
      int read = 0;
      while ((read = in.read(data, 0, data.length)) > 0) {
        out.write(data, 0, read);
      }
      return out.toByteArray();
    } finally {
      closeQuietly(in);
    }
  }

  public static int getNumJarSeparators(final String str) {
    int rv = 0;
    final int length = str.length();
    for (int i = 0; i < length; i++) {
      final char ch = str.charAt(i);
      if (ch == '!' && i < length - 1 && str.charAt(i + 1) == '/') {
        rv++;
      }
    }
    return rv;
  }

  public static URL fixUpUrl(URL source) throws IOException {
    String extForm = source.toExternalForm();

    if (extForm.startsWith("vfs") && jboss6x) {
      URLConnection conn = source.openConnection();
      Object content = conn.getContent();
      try {
        Class vfsUtilsClass = Class.forName("org.jboss.vfs.VFSUtils");
        Class virtualFileClass = Class.forName("org.jboss.vfs.VirtualFile");
        Method getPathName = virtualFileClass.getDeclaredMethod("getPathName", new Class[0]);
        Method getPhysicalURL = vfsUtilsClass.getDeclaredMethod("getPhysicalURL", virtualFileClass);
        String pathName = (String) getPathName.invoke(content, (Object[]) null);
        if (!new File(pathName).exists()) {
          // we only do this when JBoss 6.0.0 gives us an URL to the folder of the extracted jar
          // https://issues.jboss.org/browse/JBAS-8786
          Object retVal = getPhysicalURL.invoke(null, content);
          extForm = ((URL) retVal).toExternalForm();
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    if (extForm.startsWith("vfs:") && extForm.contains("jar/META-INF")) {
      extForm = extForm.replaceFirst("vfs", "jar:file").replaceFirst("jar/META-INF", "jar!/META-INF");
    }

    if (extForm.startsWith("vfs:")) {
      extForm = extForm.replaceFirst("vfs", "file");
    }

    if (extForm.startsWith("jar:") && extForm.endsWith("!/") && getNumJarSeparators(extForm) == 1) {
      extForm = extForm.substring("jar:".length(), extForm.length() - "!/".length());
    }

    if (!extForm.endsWith(".jar")) {
      // code source may return up to the class and not the containing jar

      if (extForm.startsWith("jar:") && extForm.endsWith(".class") && extForm.contains(".jar!")) {
        // rip the jar protocol, use the jar directly
        extForm = extForm.substring(4, extForm.lastIndexOf(".jar!")) + ".jar";
      }
    }
    return new URL(source, canonicalize(extForm));
  }

  private static String canonicalize(String url) throws MalformedURLException {
    String[] urlParts = url.split("\\/");

    List<String> retainedParts = new ArrayList<String>();

    for (String part : urlParts) {
      if (part.equals(".")) {
        continue;
      }

      if (part.equals("..")) {
        if (retainedParts.isEmpty()) { throw new MalformedURLException(url + " has too many \"..\" parts"); }
        retainedParts.remove(retainedParts.size() - 1);
        continue;
      }

      retainedParts.add(part);
    }

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < retainedParts.size(); i++) {
      sb.append(retainedParts.get(i));
      if (i < (retainedParts.size() - 1)) {
        sb.append("/");
      }
    }

    return sb.toString();
  }

  private static boolean isJBoss6x() {
    InputStream in = Util.class.getResourceAsStream("/org/jboss/version.properties");
    if (in == null) return false;
    Properties props = new Properties();
    try {
      props.load(in);
      if ("6".equals(props.getProperty("version.major"))) { return true; }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      closeQuietly(in);
    }
    return false;
  }

  public static boolean isDirectoryUrl(URL url) {
    File file = toFile(url);
    if (file != null && file.isDirectory()) return true;
    return false;
  }

  public static void closeQuietly(final InputStream in) {
    if (in != null) {
      try {
        in.close();
      } catch (IOException ioe) {
        // ignore
      }
    }
  }

  public static void closeQuietly(final OutputStream in) {
    if (in != null) {
      try {
        in.close();
      } catch (IOException ioe) {
        // ignore
      }
    }
  }

  public static int copy(final InputStream in, final OutputStream out) throws IOException {
    byte[] buffer = new byte[1024 * 4];
    int count = 0;
    int n = 0;
    while (-1 != (n = in.read(buffer))) {
      out.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  public static void copyFile(final File srcFile, final File destFile) throws IOException {
    FileInputStream input = new FileInputStream(srcFile);
    try {
      FileOutputStream output = new FileOutputStream(destFile);
      try {
        copy(input, output);
      } finally {
        closeQuietly(output);
      }
    } finally {
      closeQuietly(input);
    }
  }

  public static URL toURL(final File file) throws MalformedURLException {
    return file.toURI().toURL();
  }

  public static File toFile(URL url) {
    if (!url.toExternalForm().startsWith("file")) { return null; }
    String path = url.getPath();
    try {
      return new File(URLDecoder.decode(path, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      return null;
    }
  }
}
