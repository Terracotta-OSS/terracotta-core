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

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.terracotta.configuration.Directories;
import com.tc.util.ManagedServiceLoader;
import java.io.File;
import java.io.FileNotFoundException;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


/**
 * Top level service locator class used to identify and isolate service dependencies in its own classloader.
 */
public class ServiceLocator extends ManagedServiceLoader {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceLocator.class);
  private static final boolean STRICT = !TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L2_CLASSLOADER_COMPATIBILITY);
  private final ClassLoader defaultClassLoader;

  public ServiceLocator(ClassLoader parent) {
    defaultClassLoader = parent;
  }
  
  @Override
  protected Class<?> loadClass(String className, String location, ClassLoader loader) {
    try {
      return Class.forName(className, true, new ComponentURLClassLoader(className, new URL[] {new URL(location)}, loader,new AnnotationOrDirectoryStrategyChecker()));
    } catch (MalformedURLException aml) {
      LOG.warn("unable to load " + className + " from " + location, aml);
    } catch  (ClassNotFoundException nf) {
      LOG.warn("unable to load " + className + " from " + location, nf);
    }
    return super.loadClass(className, location, loader);
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
      LOG.info("Entity/Service apis will be loaded from " + Directories.getServerPluginsApiDir().getAbsolutePath());
      LOG.info("Entity/Service implementations will be loaded from " + Directories.getServerPluginsLibDir().getAbsolutePath());
      URLClassLoader purls = new StrictURLClassLoader(findPluginURLS(), createApiClassLoader(ServiceLocator.class.getClassLoader()),new AnnotationOrDirectoryStrategyChecker(),STRICT);
      return purls;
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