package com.tc.classloader;


import org.junit.Assert;
import org.junit.Test;
import org.terracotta.entity.Service;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ServiceLocatorTest {

  private static final URL rootDir = ServiceLocatorTest.class.getClassLoader().getResource("classloader-tests");

  /*
    * This method show cases how we should limit the scope of platform class loader, we can have an "ext" folder according
    * java standards.
   */
  private URL[] getListOfJars() {
    try {
      File f = new File(rootDir.toURI());
      String[] uris = f.list(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          return name.endsWith(".jar");
        }
      });
      URL[] toRet = new URL[uris.length];
      for (int i = 0; i < uris.length; i++) {
        toRet[i] = new URL(rootDir.toString() + File.separator + uris[i]);
      }
      return toRet;
    } catch (URISyntaxException e) {
      //ignore
    } catch (MalformedURLException e) {
      //ignore
    }
    return null;
  }

  @Test
  public void testServiceProviderDiscovery() {
    //Limit scope of search
    URL[] listOfJars = getListOfJars();
    Assert.assertNotNull("test jars not located at " + rootDir, listOfJars);
    URLClassLoader parent = new URLClassLoader(listOfJars);

    //Check right amount of files are discovered
    Map<String, String> implJars = ServiceLocator.discoverImplementations(parent, ServiceProvider.class.getName());
    Assert.assertNotNull("No implementation jars found!", implJars);
    Assert.assertEquals("Two implementations have to be found!", 2, implJars.size());
  }

  @Test
  public void testServiceProviderInstantiation() {
    //limit the scope of search
    URL[] listOfJars = getListOfJars();
    Assert.assertNotNull("test jars not located at " + rootDir, listOfJars);
    final URLClassLoader parent = new URLClassLoader(listOfJars);

    //Check if right implementations are created with right loaders
    List<ServiceProvider> providers = ServiceLocator.getImplementations(ServiceProvider.class, parent);

    Assert.assertEquals("Two implementations have to be found!", 2, providers.size());

    Set<ClassLoader> classLoaderSet = new HashSet<ClassLoader>();
    for (ServiceProvider p : providers) {
      ClassLoader loader = p.getClass().getClassLoader();
      //this is required so that we know that system class loader is used
      Assert.assertTrue("Check if right class loader is used",loader instanceof ComponentLoader);
      //Make sure that we are not loading both interface and implementation at same scope
      Assert.assertNotEquals(ServiceProvider.class.getClassLoader(), loader);
      //add to set to check number of implementation to check uniques across implementations.
      classLoaderSet.add(loader);
    }

    Assert.assertEquals("Unique class loader should be assigned to each of providers ",
        classLoaderSet.size(), providers.size());

  }

  @Test
  public void testHierarchicalLoading() throws Exception {
    //limit the scope of search
    URL[] listOfJars = getListOfJars();
    Assert.assertNotNull("test jars not located at " + rootDir, listOfJars);
    final URLClassLoader parent = new URLClassLoader(listOfJars);

    //Check if right implementations are created with right loaders
    List<ServiceProvider> providers = ServiceLocator.getImplementations(ServiceProvider.class, parent);
    Assert.assertEquals("Two implementations have to be found!", 2, providers.size());


    //XXX introspective code to test hirearchy
    for (ServiceProvider p : providers) {
      Service<Object> s = p.getService(1, new ServiceConfiguration<Object>() {
        @Override
        public Class<Object> getServiceType() {
          try {
            return (Class<Object>) Class.forName("com.terracotta.sample.service.IClassLoading", false, parent);
          } catch (ClassNotFoundException e) {
            e.printStackTrace();
          }
          return null;
        }
      });
      Object o = s.get();
      Class<?> aClass = o.getClass();
      Method gcl = aClass.getDeclaredMethod("getClassLoader");
      gcl.setAccessible(true);
      Method gpl = aClass.getDeclaredMethod("getParentTypeLoader");
      gpl.setAccessible(true);
      //IClassloading defines two methods to return implementation classloader and type class loader
      Assert.assertNotEquals("Concrete implementation and API should not be having same classloader", gcl.invoke(o), gpl.invoke(o));
      Assert.assertEquals("Check the parent loader which we create for loading components are followed", gpl.invoke(o), parent);
    }
  }

}
