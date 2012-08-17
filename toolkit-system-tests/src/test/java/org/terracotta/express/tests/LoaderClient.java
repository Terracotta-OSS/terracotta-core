/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFactory;
import org.terracotta.toolkit.ToolkitInstantiationException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoaderClient {
  private static final int INDEX_WHERE_TO_SNEAK_DOT_DOT = 2;

  public static void main(String[] args) throws Exception {
    System.setProperty("terracottaUrl", args[0]);

    Map<String, byte[]> extra = new HashMap<String, byte[]>();
    extra.put(Asserter.class.getName(), getClassBytes(Asserter.class));

    URL testUrl = jarFor(LoaderClient.class);
    URL toolkitUrl = jarFor(ToolkitFactory.class);
    toolkitUrl = sneakDotDotInUrl(toolkitUrl);

    Loader loader = new Loader(new URL[] { toolkitUrl, testUrl }, null, extra);
    Runnable r = (Runnable) loader.loadClass(Asserter.class.getName()).newInstance();
    r.run();

    System.out.println("[PASS: " + LoaderClient.class.getName() + "]");
  }

  private static URL sneakDotDotInUrl(URL url) throws MalformedURLException {
    String path = url.getPath();
    String protocol = url.getProtocol();
    List<String> pathEntries = new ArrayList<String>(Arrays.asList(path.split("\\/")));

    if (pathEntries.size() < INDEX_WHERE_TO_SNEAK_DOT_DOT + 1) { throw new AssertionError(
                                                                                          "path to url ("
                                                                                              + url
                                                                                              + ") is too short - it must contain at least "
                                                                                              + (INDEX_WHERE_TO_SNEAK_DOT_DOT + 1)
                                                                                              + " path separators"); }

    String entry = pathEntries.get(INDEX_WHERE_TO_SNEAK_DOT_DOT);
    pathEntries.add(INDEX_WHERE_TO_SNEAK_DOT_DOT + 1, "..");
    pathEntries.add(INDEX_WHERE_TO_SNEAK_DOT_DOT + 2, entry);

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < pathEntries.size(); i++) {
      String s = pathEntries.get(i);
      sb.append(s);
      if (i < pathEntries.size() - 1) sb.append("/");
    }
    path = sb.toString();

    return new URL(protocol + ":" + path);
  }

  private static URL jarFor(Class c) {
    ProtectionDomain protectionDomain = c.getProtectionDomain();
    CodeSource codeSource = protectionDomain.getCodeSource();
    return codeSource.getLocation();
  }

  private static byte[] getClassBytes(Class<?> clazz) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    InputStream in = LoaderClient.class.getClassLoader().getResourceAsStream(clazz.getName().replace('.', '/')
                                                                                 .concat(".class"));

    int b;
    while ((b = in.read()) >= 0) {
      baos.write(b);
    }

    return baos.toByteArray();
  }

  public static class Loader extends URLClassLoader {

    private final Map<String, byte[]> extra;

    public Loader(URL[] urls, ClassLoader parent, Map<String, byte[]> extra) {
      super(urls, parent);
      this.extra = extra;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
      byte[] b = extra.remove(name);
      if (b != null) { return defineClass(name, b, 0, b.length); }

      return super.findClass(name);
    }

  }

  public static class Asserter implements Runnable {

    public void run() {
      String terracottaUrl = System.getProperty("terracottaUrl");
      String mapName = "__" + LoaderClient.class.getName().replace('.', '_') + "_clusteredmap";

      Toolkit toolkit = createToolkit(terracottaUrl);
      System.err.println("clustered map: " + toolkit.getMap(mapName, null, null));
    }

    private Toolkit createToolkit(String terracottaUrl) {
      try {
        return ToolkitFactory.createToolkit("toolkit:terracotta://" + terracottaUrl);
      } catch (ToolkitInstantiationException e) {
        throw new RuntimeException(e);
      }
    }
  }

}
