/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFactory;

import java.net.URL;
import java.net.URLClassLoader;

import javax.transaction.TransactionRequiredException;
import javax.transaction.xa.Xid;

public class JtaClassesClient extends ClientBase {

  public JtaClassesClient(String[] args) {
    super(args);
  }

  public static void main(String[] args) {
    new JtaClassesClient(args).run();
  }

  @Override
  public void test(Toolkit DONT_USE_ME) throws Throwable {

    // boot up a toolkit client and create a clustered map with it in another loader
    ClassLoader webAppLoader = new Loader();
    System.setProperty("SecretProvider.secret", "lala"); // Required for testing security, creating a new Client would
                                                         // have you prompted for the password again
    Class<?> tkClass = webAppLoader.loadClass(ToolkitFactory.class.getName());
    String fullTCUrl = "toolkit:terracotta://" + getTerracottaUrl();
    Object toolkit = tkClass.getMethod("createToolkit", String.class).invoke(tkClass, fullTCUrl);
    Object map = toolkit.getClass().getMethod("getMap", String.class, Class.class, Class.class).invoke(toolkit, "foo", Object.class, Object.class);

    ClassLoader clusteredLoader = map.getClass().getClassLoader();

    // Xid class should come from web app loader (not bootstrap loader)
    Class<?> xidClass = clusteredLoader.loadClass(Xid.class.getName());
    if (xidClass.getClassLoader() != webAppLoader) { throw new AssertionError(xidClass.getClassLoader()); }

    // for good measure make sure that Xid is indeed in the bootclasspath
    Class vmXidClass = Class.forName(Xid.class.getName());
    if (vmXidClass.getClassLoader() != null) { throw new AssertionError(vmXidClass.getClassLoader()); }

    // check another class for good measure
    Class<?> txnReqdClass = clusteredLoader.loadClass(TransactionRequiredException.class.getName());
    if (txnReqdClass.getClassLoader() != webAppLoader) { throw new AssertionError(txnReqdClass.getClassLoader()); }
  }

  private static class Loader extends URLClassLoader {

    public Loader() {
      super(getSystemUrls(), null);
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      Class c = findLoadedClass(name);

      // emulate a web app loader and look local before delegating to parent
      // classes starting with java. should be loaded by the system class loader only.
      if (c == null && !name.startsWith("java.")) {
        try {
          c = findClass(name);
        } catch (ClassNotFoundException cnfe) {
          //
        }
      }

      if (c == null) {
        try {
          c = Class.forName(name, false, getParent());
        } catch (ClassNotFoundException cnfe) {
          //
        }
      }

      if (c != null) {
        if (resolve) {
          resolveClass(c);
        }

        return c;
      }

      throw new ClassNotFoundException(name);
    }

    private static URL[] getSystemUrls() {
      return ((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs();
    }
  }

}
