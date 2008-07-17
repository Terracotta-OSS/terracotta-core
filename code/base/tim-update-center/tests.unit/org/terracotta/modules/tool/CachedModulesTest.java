/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool;

import java.io.InputStream;
import java.util.List;

import junit.framework.TestCase;

public class CachedModulesTest extends TestCase {

  public void testList() {
    Modules modules = getModules("9.9.9", "/testList.xml");
    assertNotNull(modules);
    assertTrue(modules.list().isEmpty());

    modules = getModules("2.5.0", "/testList.xml");
    assertNotNull(modules);
    assertFalse(modules.list().isEmpty());
    assertEquals(2, modules.list().size());
    
    modules = getModules("2.5.2", "/testList.xml");
    assertNotNull(modules);
    assertFalse(modules.list().isEmpty());
    assertEquals(2, modules.list().size());
    
    modules = getModules("2.5.4", "/testList.xml");
    assertNotNull(modules);
    assertFalse(modules.list().isEmpty());
    assertEquals(4, modules.list().size());

    modules = getModules("2.6.0", "/testList.xml");
    assertNotNull(modules);
    assertFalse(modules.list().isEmpty());
    assertEquals(1, modules.list().size());

    modules = getModules("2.6.1", "/testList.xml");
    assertNotNull(modules);
    assertFalse(modules.list().isEmpty());
    assertEquals(2, modules.list().size());

    modules = getModules("2.6.2", "/testList.xml");
    assertNotNull(modules);
    assertFalse(modules.list().isEmpty());
    assertEquals(2, modules.list().size());
  }
  
  public void testListLatest() {
    Modules modules = getModules("2.5.0", "/testList.xml");
    assertNotNull(modules);
    assertFalse(modules.list().isEmpty());
    assertEquals(2, modules.list().size());
    
    List<Module> list = modules.get("org.foo.bar", "tim-annotations");
    assertFalse(list.isEmpty());
    assertTrue(list.get(0).getId().getVersion().equals("1.0.0"));
    assertTrue(list.get(1).getId().getVersion().equals("1.0.1"));
    assertTrue(list.get(1).isLatest());
    
    List<Module> latest = modules.listLatest();
    assertNotNull(latest);
    assertFalse(latest.isEmpty());
    assertEquals(1, latest.size());
    assertTrue(latest.get(0).getId().getVersion().equals("1.0.1"));
    assertTrue(latest.get(0).isLatest());
  }
  
  public void testGetUsingId() {
    Modules modules = getModules("2.5.0", "/testList.xml");
    assertNotNull(modules);
    assertFalse(modules.list().isEmpty());
    assertEquals(2, modules.list().size());
    
    ModuleId id = ModuleId.create("org.foo.bar", "tim-annotations", "1.0.0");
    Module module = modules.get(id);
    assertNotNull(module);
    assertTrue(module.getId().equals(id));
    
    id = ModuleId.create("org.foo.bar", "tim-annotations", "1.0.1");
    module = modules.get(id);
    assertNotNull(module);
    assertTrue(module.getId().equals(id));
  }
  
  public void testGetUsingGroupIdAndArtifactId() {
    Modules modules = getModules("2.5.0", "/testList.xml");
    assertNotNull(modules);
    assertFalse(modules.list().isEmpty());
    assertEquals(2, modules.list().size());
    
    List<Module> list = modules.get("org.foo.bar", "tim-annotations");
    assertNotNull(list);
    assertEquals(2, list.size());
    for (Module module : list) {
      assertTrue(module.getSymbolicName().equals(ModuleId.computeSymbolicName("org.foo.bar", "tim-annotations")));
    }
  }
  
  public void testGetLatest() {
    Modules modules = getModules("2.5.4", "/testList.xml");
    assertNotNull(modules);
    assertFalse(modules.list().isEmpty());
    assertEquals(4, modules.list().size());
    List<Module> list = modules.get("org.foo.bar", "tim-apache-struts-1.1");
    assertNotNull(list);
    assertFalse(list.isEmpty());
    assertEquals(3, list.size());
    Module latest = modules.getLatest("org.foo.bar", "tim-apache-struts-1.1");
    assertNotNull(latest);
    assertTrue(latest.isLatest());
    assertTrue(latest.getId().getVersion().equals("1.0.3"));
  }
  
  private Modules getModules(String tcversion, String file) {
    try {
      InputStream data = this.getClass().getResourceAsStream(file);
      assertNotNull(data);
      return new CachedModules(tcversion, data);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
