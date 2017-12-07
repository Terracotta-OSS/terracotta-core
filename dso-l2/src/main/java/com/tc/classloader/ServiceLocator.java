/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */

package com.tc.classloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.config.Directories;
import com.tc.util.ManagedServiceLoader;
import java.io.File;
import java.io.FileNotFoundException;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;


/**
 * Top level service locator class used to identify and isolate service dependencies in its own classloader.
 */
public class ServiceLocator extends ManagedServiceLoader {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceLocator.class);
  
  private final ClassLoader defaultClassLoader;

  static {
    try {
      LOG.info("Entity/Service apis will be loaded from " + Directories.getServerPluginsApiDir().getAbsolutePath());
      LOG.info("Entity/Service implementations will be loaded from " + Directories.getServerPluginsLibDir().getAbsolutePath());
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public ServiceLocator(ClassLoader parent) {
    defaultClassLoader = parent;
  }
  
  @Override
  protected Class<?> loadClass(String className, String location, ClassLoader loader) {
    try {
      return Class.forName(className, false, new ComponentURLClassLoader(new URL[] {new URL(location)}, getApiClassLoader(loader), new AnnotationOrDirectoryStrategyChecker()));
    } catch (MalformedURLException aml) {
      LOG.warn("unable to load " + className + " from " + location, aml);
    } catch  (ClassNotFoundException nf) {
      LOG.warn("unable to load " + className + " from " + location, nf);
    }
    return super.loadClass(className, location, loader);
  }
  
  
  private final Map<ClassLoader, ApiClassLoader> API_LOADERS = new WeakHashMap<>();
  
  private synchronized ApiClassLoader getApiClassLoader(ClassLoader parent) {
    ClassLoader chainCheck = parent;
// first walk the chain to see if there are any ApiClassLoaders.  If so, Use that
    while (chainCheck != null) {
      if (chainCheck instanceof ApiClassLoader) {
        return (ApiClassLoader)chainCheck;
      } else {
        chainCheck = chainCheck.getParent();
      }
    }
//  remove a possible ComponentURLClassLoader
    chainCheck = parent;
    while (chainCheck instanceof ComponentURLClassLoader) {
      chainCheck = chainCheck.getParent();
    }
    if (chainCheck == null) {
      chainCheck = ClassLoader.getSystemClassLoader();
    }
// see if there is already a class
    ApiClassLoader loader = API_LOADERS.get(chainCheck);
    if (loader == null) {
      loader = createApiClassLoader(chainCheck);
      API_LOADERS.put(chainCheck, loader);
    }
    return loader;
  }
  
  private static boolean fileFilter(File target) {
    String name = target.getName().toLowerCase();
    return name.endsWith(".jar") || name.endsWith(".zip");
  }
  
  private static URL toURL(File uri) {
    try {
      return uri.toURI().toURL();
    } catch (MalformedURLException mal) {
      return null;
    }
  }
      
  private static URL[] createURLS(File plugins) {
    if (plugins.exists()) {
      return Arrays.stream(plugins.listFiles())
        .filter(ServiceLocator::fileFilter)
        .map(ServiceLocator::toURL)
        .filter(u->u!=null)
        .toArray(i->new URL[i]);
    }
    return new URL[0];
  }
  
  private static URL[] findPluginURLS() throws FileNotFoundException {
    return createURLS(Directories.getServerPluginsLibDir());
  }
  
  public static ClassLoader PLATFORM_LOADER = createPlatformClassLoader();
  
  public static ClassLoader getPlatformLoader() {
    return PLATFORM_LOADER;
  }
  
  private static ClassLoader createPlatformClassLoader() {
    try {
      URLClassLoader purls = new URLClassLoader(findPluginURLS());
      ApiClassLoader apis = createApiClassLoader(purls);
      return apis;
    } catch (FileNotFoundException file) {
      return ClassLoader.getSystemClassLoader();
    }
  }

  private static ApiClassLoader createApiClassLoader(ClassLoader parent) {
    try {
      return new ApiClassLoader(createURLS(Directories.getServerPluginsApiDir()), parent);
    } catch (FileNotFoundException notfound) {
      return new ApiClassLoader(new URL[0], parent);
    }
  }
  
  Collection<Class<?>> testingCheckUrls(String interfaceName) {
    return discoverImplementations(interfaceName, defaultClassLoader);
  }

  public <T> List<Class<? extends T>> getImplementations(Class<T> interfaceClass) {
    return super.getImplementations(interfaceClass, defaultClassLoader); 
  }
}