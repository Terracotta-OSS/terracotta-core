/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package sun.misc;

import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;

import java.net.URL;

/**
 * This class is merged with sun.misc.Launcher$AppClassLoader (which is the implenetation type the system classloader in
 * Sun VM). We add a call in it's findClass() to make sure that exported classes destined only for the system loader are
 * properly located
 */
public class AppClassLoaderTC extends Launcher.AppClassLoader {

  AppClassLoaderTC(URL[] urls, ClassLoader parent) {
    super(urls, parent);
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    byte[] data = ClassProcessorHelper.systemLoaderFindClassHook(name, this);
    if (data != null) { return defineClass(name, data, 0, data.length); }

    return super.findClass(name);
  }

}
