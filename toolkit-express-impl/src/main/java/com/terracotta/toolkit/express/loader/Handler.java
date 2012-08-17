/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express.loader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler {

  public static final String       TAG                        = "__TC__";
  public static final String       TC_JAR_PROTOCOL            = "tcjar";

  private static final IOException NO_SUCH_RESOURCE_EXCEPTION = new IOException("no such resource");

  private final JarManager         jarManager;

  public Handler(JarManager jarManager) {
    this.jarManager = jarManager;
  }

  @Override
  protected URLConnection openConnection(final URL url) throws IOException {
    String proto = url.getProtocol();
    if (!TC_JAR_PROTOCOL.equals(proto)) { throw new IOException("unexpected protocol: " + url); }

    String path = url.getPath();

    if (!path.startsWith(TAG)) { throw new IOException("unexpected path: " + path); }

    path = path.substring(TAG.length());

    int end = path.indexOf(TAG);
    if (end <= 0) { throw new IOException("unexpected path: " + path); }

    String jarKey = path.substring(0, end);
    String resource = path.substring(end + TAG.length());

    if (resource.startsWith("/")) {
      resource = resource.substring(1);
    }

    Jar jar = jarManager.get(jarKey);
    if (jar == null) { throw new IOException("No source for: " + url); }

    if (resource.length() == 0) { return new Connection(url, jar.contents()); }

    byte[] data = jar.lookup(resource);
    if (data == null) {
      // this is a very common path -- not constructing a detailed error here
      throw NO_SUCH_RESOURCE_EXCEPTION;
    }

    return new Connection(url, data);
  }

  private static class Connection extends URLConnection {
    private final byte[] content;

    public Connection(URL url, byte[] content) {
      super(url);
      this.content = content;
    }

    @Override
    public void connect() {
      // nothing to do
    }

    @Override
    public InputStream getInputStream() {
      return new ByteArrayInputStream(content);
    }
  }

}
