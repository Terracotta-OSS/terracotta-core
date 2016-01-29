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

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
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
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.ServerEntityService;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceRegistry;


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
    List<Class<? extends ServiceProvider>> providers = ServiceLocator.getImplementations(ServiceProvider.class, parent);

    Assert.assertEquals("Two implementations have to be found!", 2, providers.size());

    Set<ClassLoader> classLoaderSet = new HashSet<ClassLoader>();
    for (Class<? extends ServiceProvider> p : providers) {
      ClassLoader loader = p.getClassLoader();
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
    List<Class<? extends ServiceProvider>> providers = ServiceLocator.getImplementations(ServiceProvider.class, parent);
    Assert.assertEquals("Two implementations have to be found!", 2, providers.size());

    //XXX introspective code to test hirearchy
    for (Class<? extends ServiceProvider> p : providers) {
      ServiceConfiguration<Object> serviceConfiguration = new ServiceConfiguration<Object>() {
        @Override
        public Class<Object> getServiceType() {
          try {
            return (Class<Object>) Class.forName("com.terracotta.sample.service.IClassLoading", false, parent);
          } catch (ClassNotFoundException e) {
            e.printStackTrace();
          }
          return null;
        }
      };
      Object o = p.newInstance().getService(1, serviceConfiguration);
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
/* disabling this test for now.  refactoring makes the test jars invalid.  Need to find
  a better way to test this functionality
  */
  @Test @Ignore 
  public void testEntityServiceLoadingHierarchical() throws Exception {
    //limit the scope of search
    URL[] listOfJars = getListOfJars();
    Assert.assertNotNull("test jars not located at " + rootDir, listOfJars);
    final URLClassLoader parent = new URLClassLoader(listOfJars);

    //Check if right implementations are created with right loaders
    final List<Class<? extends ServiceProvider>> providers = ServiceLocator.getImplementations(ServiceProvider.class, parent);
    Assert.assertEquals("Two implementations have to be found!", 2, providers.size());

    final Map<Class<?>, List<ServiceProvider>> serviceProviderMap = new HashMap<Class<?>, List<ServiceProvider>>();

    for (Class<? extends ServiceProvider> pclass : providers) {
      ServiceProvider p = pclass.newInstance();
      for (Class<?> serviceType : p.getProvidedServiceTypes()) {
        List<ServiceProvider> listForType = serviceProviderMap.get(serviceType);
        if (null == listForType) {
          listForType = new Vector<ServiceProvider>();
          serviceProviderMap.put(serviceType, listForType);
        }
        listForType.add(p);
      }
    }
    ServiceRegistry registry = new ServiceRegistry() {
      @Override
      public <T> T getService(ServiceConfiguration<T> serviceConfiguration) {
        List<ServiceProvider> providers1 = serviceProviderMap.get(serviceConfiguration.getServiceType());
        if(providers1.isEmpty()) {
          Assert.fail("Entity queried for something which does not exist, this should never happen!!!");
        }
        return providers1.get(0).getService(1, serviceConfiguration);
      }
    };

    //We are not testing service to service hierarchy loading as it is tested in other test case

    //discover entities & inject service!!
    @SuppressWarnings("rawtypes")
    List<Class<? extends ServerEntityService>> entityServices = ServiceLocator.getImplementations(ServerEntityService.class, parent);
    for (@SuppressWarnings("rawtypes") Class<? extends ServerEntityService> es : entityServices) {
      handleService(parent, registry, es.newInstance());
    }
  }

  private <A extends EntityMessage, R extends EntityResponse> void handleService(URLClassLoader parent, ServiceRegistry registry, ServerEntityService<ActiveServerEntity<A, R>, ?> es) {
    ActiveServerEntity<A, R> activeEntity = es.createActiveEntity(registry, null);
    MessageCodec<A, R> codec = activeEntity.getMessageCodec();
    //get class name of IClassLoader type
    R gpl = activeEntity.invoke(null, codec.deserialize("gpl".getBytes()));
    //get class name of the entity loader
    R gel = activeEntity.invoke(null, codec.deserialize("gel".getBytes()));
    //get reference of the IClassloader loader
    R plr = activeEntity.invoke(null, codec.deserialize("plr".getBytes()));
    Assert.assertNotEquals("Entity classloader and parent(IClassLoader) loader should not be same", gpl, gel);
    //XXX works only because on same VM
    Assert.assertEquals("Same parent loader reference is used to load (Iclassloader)", plr,
        Integer.toString(System.identityHashCode(parent)));
  }

  @Test
  public void testMultipleServiceImplInSameJar() throws Exception {
    //limit the scope of search
    URL[] listOfJars = getListOfJars();
    Assert.assertNotNull("test jars not located at " + rootDir, listOfJars);
    final URLClassLoader parent = new URLClassLoader(listOfJars);

    //Check if right implementations are created with right loaders
    final List<Class<?>> providers = (List<Class<?>>) ServiceLocator.getImplementations(Class.forName("com.terracotta.sample.service.MyService",
        true, parent), parent);

    //We use same service names in 2 jars, so we discover only 2 implementation, which is normal ServiceLoader in java
    //behaviour.
    Assert.assertEquals(2, providers.size());
  }

}
