package com.tc.classloader;


import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import org.junit.Ignore;
import org.terracotta.entity.ServiceProvider;

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

/* disabling this test for now.  refactoring makes the test jars invalid.  Need to find
  a better way to test this functionality
  */
  @Test @Ignore 
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
      Assert.assertTrue("Check if right class loader is used", loader instanceof ComponentURLClassLoader);
      //Make sure that we are not loading both interface and implementation at same scope
      Assert.assertNotEquals(ServiceProvider.class.getClassLoader(), loader);
      //add to set to check number of implementation to check uniques across implementations.
      classLoaderSet.add(loader);
    }

    Assert.assertEquals("Unique class loader should be assigned to each of providers ",
        classLoaderSet.size(), providers.size());

  }

/* disabling this test for now.  refactoring makes the test jars invalid.  Need to find
  a better way to test this functionality
  */
  @Test @Ignore 
  public void testHierarchicalServiceLoading() throws Exception {
    //limit the scope of search
    URL[] listOfJars = getListOfJars();
    Assert.assertNotNull("test jars not located at " + rootDir, listOfJars);
    final URLClassLoader parent = new URLClassLoader(listOfJars);

    //Check if right implementations are created with right loaders
    List<ServiceProvider> providers = ServiceLocator.getImplementations(ServiceProvider.class, parent);
    Assert.assertEquals("Two implementations have to be found!", 2, providers.size());

    //XXX introspective code to test hirearchy
    for (ServiceProvider p : providers) {
//      ServiceConfiguration<Object> serviceConfiguration = new ServiceConfiguration<Object>() {
//        @Override
//        public Class<Object> getServiceType() {
//          try {
//            return (Class<Object>) Class.forName("com.terracotta.sample.service.IClassLoading", false, parent);
//          } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//          }
//          return null;
//        }
//      };
//      Object o = p.getService(1, serviceConfiguration);
//      Class<?> aClass = o.getClass();
//      Method gcl = aClass.getDeclaredMethod("getClassLoader");
//      gcl.setAccessible(true);
//      Method gpl = aClass.getDeclaredMethod("getParentTypeLoader");
//      gpl.setAccessible(true);
//      //IClassloading defines two methods to return implementation classloader and type class loader
//      Assert.assertNotEquals("Concrete implementation and API should not be having same classloader", gcl.invoke(o), gpl.invoke(o));
//      Assert.assertEquals("Check the parent loader which we create for loading components are followed", gpl.invoke(o), parent);
    }
  }
/* disabling this test for now.  refactoring makes the test jars invalid.  Need to find
  a better way to test this functionality
  */
  @Test @Ignore 
  public void testEntityServiceLoadingHierarchical() {
    //limit the scope of search
    URL[] listOfJars = getListOfJars();
    Assert.assertNotNull("test jars not located at " + rootDir, listOfJars);
    final URLClassLoader parent = new URLClassLoader(listOfJars);

    //Check if right implementations are created with right loaders
    final List<ServiceProvider> providers = ServiceLocator.getImplementations(ServiceProvider.class, parent);
    Assert.assertEquals("Two implementations have to be found!", 2, providers.size());

    final Map<Class<?>, List<ServiceProvider>> serviceProviderMap = new HashMap<Class<?>, List<ServiceProvider>>();

    for (ServiceProvider p : providers) {
      for (Class<?> serviceType : p.getProvidedServiceTypes()) {
        List<ServiceProvider> listForType = serviceProviderMap.get(serviceType);
        if (null == listForType) {
          listForType = new Vector<ServiceProvider>();
          serviceProviderMap.put(serviceType, listForType);
        }
        listForType.add(p);
      }
    }
//    ServiceRegistry registry = new ServiceRegistry() {
//      @Override
//      public <T> T getService(ServiceConfiguration<T> serviceConfiguration) {
//        List<ServiceProvider> providers1 = serviceProviderMap.get(serviceConfiguration.getServiceType());
//        if(providers1.isEmpty()) {
//          Assert.fail("Entity queried for something which does not exist, this should never happen!!!");
//        }
//        return providers1.get(0).getService(1, serviceConfiguration);
//      }
//    };

    //We are not testing service to service hirearchy loading as it is tested in other test case

    //discover entities & inject service!!
//    List<ServerEntityService> entityServices = ServiceLocator.getImplementations(ServerEntityService.class, parent);
//    for (ServerEntityService es : entityServices) {
//      ActiveServerEntity activeEntity = es.createActiveEntity(registry, null);
//      //get class name of IClassLoader type
//      String gpl = new String(activeEntity.invoke(null, "gpl".getBytes()));
//      //get class name of the entity loader
//      String gel = new String(activeEntity.invoke(null, "gel".getBytes()));
//      //get reference of the IClassloader loader
//      String plr = new String(activeEntity.invoke(null, "plr".getBytes()));
//      Assert.assertNotEquals("Entity classloader and parent(IClassLoader) loader should not be same", gpl, gel);
//      //XXX works only because on same VM
//      Assert.assertEquals("Same parent loader reference is used to load (Iclassloader)", plr,
//          Integer.toString(System.identityHashCode(parent)));
//    }
  }

  @Test
  public void testMultipleServiceImplInSameJar() throws Exception {
    //limit the scope of search
    URL[] listOfJars = getListOfJars();
    Assert.assertNotNull("test jars not located at " + rootDir, listOfJars);
    final URLClassLoader parent = new URLClassLoader(listOfJars);

    //Check if right implementations are created with right loaders
    final List<Object> providers = (List<Object>) ServiceLocator.getImplementations(Class.forName("com.terracotta.sample.service.MyService",
        true, parent), parent);

    //We use same service names in 2 jars, so we discover only 2 implementation, which is normal ServiceLoader in java
    //behaviour.
    Assert.assertEquals(2, providers.size());
  }

}
