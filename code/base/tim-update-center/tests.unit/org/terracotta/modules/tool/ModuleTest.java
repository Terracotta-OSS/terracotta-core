/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.terracotta.modules.tool.util.DataLoader;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import junit.framework.TestCase;

public class ModuleTest extends TestCase {

  public void testDependencies() {
    Modules modules = getModules("9.9.9", "/testInstall.xml");
    assertNotNull(modules);
    assertFalse(modules.list().isEmpty());
    assertEquals(15, modules.list().size());
    
    ModuleId id = ModuleId.create("org.foo.bar", "with-no-dependencies", "1.0.0");
    Module module = modules.get(id);
    assertNotNull(module);
    List<ModuleId> manifest = module.dependencies();
    assertTrue(manifest.isEmpty());
    
    id = ModuleId.create("org.foo.bar", "with-one-direct-dependency", "1.0.0");
    module = modules.get(id);
    assertNotNull(module);
    manifest = module.dependencies();
    assertFalse(manifest.isEmpty());
    assertEquals(1, manifest.size());
    assertTrue(manifest.get(0).equals(ModuleId.create("org.foo.bar", "direct-dependency", "1.0.0")));

    id = ModuleId.create("org.foo.bar", "with-one-direct-and-one-referenced-dependency", "1.0.0");
    module = modules.get(id);
    assertNotNull(module);
    manifest = module.dependencies();
    assertFalse(manifest.isEmpty());
    assertEquals(2, manifest.size());
    assertTrue(manifest.get(0).equals(ModuleId.create("org.foo.bar", "direct-dependency", "1.0.0")));
    assertTrue(manifest.get(1).equals(ModuleId.create("org.foo.bar", "referenced-dependency", "1.0.0")));
    
    id = ModuleId.create("org.foo.bar", "with-one-direct-and-many-referenced-dependency", "1.0.0");
    module = modules.get(id);
    assertNotNull(module);
    manifest = module.dependencies();
    assertFalse(manifest.isEmpty());
    assertEquals(5, manifest.size());
    assertTrue(manifest.get(0).equals(ModuleId.create("org.foo.bar", "direct-dependency", "1.0.0")));
    assertTrue(manifest.get(1).equals(ModuleId.create("org.foo.bar", "referenced-dependency", "1.0.0")));
    assertTrue(manifest.get(2).equals(ModuleId.create("org.foo.bar", "referenced-dependency", "1.0.1")));
    assertTrue(manifest.get(3).equals(ModuleId.create("org.foo.bar", "referenced-dependency", "1.0.2")));
    assertTrue(manifest.get(4).equals(ModuleId.create("org.foo.bar", "referenced-dependency", "1.0.3")));

    id = ModuleId.create("org.foo.bar", "with-many-direct-and-many-referenced-dependency", "1.0.0");
    module = modules.get(id);
    assertNotNull(module);
    manifest = module.dependencies();
    assertFalse(manifest.isEmpty());
    assertEquals(7, manifest.size());
    assertTrue(manifest.get(0).equals(ModuleId.create("org.foo.bar", "direct-dependency", "1.0.0")));
    assertTrue(manifest.get(1).equals(ModuleId.create("org.foo.bar", "direct-dependency", "1.0.1")));
    assertTrue(manifest.get(2).equals(ModuleId.create("org.foo.bar", "direct-dependency", "1.0.2")));
    assertTrue(manifest.get(3).equals(ModuleId.create("org.foo.bar", "referenced-dependency", "1.0.0")));
    assertTrue(manifest.get(4).equals(ModuleId.create("org.foo.bar", "referenced-dependency", "1.0.1")));
    assertTrue(manifest.get(5).equals(ModuleId.create("org.foo.bar", "referenced-dependency", "1.0.2")));
    assertTrue(manifest.get(6).equals(ModuleId.create("org.foo.bar", "referenced-dependency", "1.0.3")));

    id = ModuleId.create("org.foo.bar", "with-direct-and-deep-referenced-dependencies", "1.0.0");
    module = modules.get(id);
    assertNotNull(module);
    manifest = module.dependencies();
    assertFalse(manifest.isEmpty());
    assertEquals(4, manifest.size());
    assertTrue(manifest.get(0).equals(ModuleId.create("org.foo.bar", "deep-referenced-dependency", "1.0.0")));
    assertTrue(manifest.get(1).equals(ModuleId.create("org.foo.bar", "deep-referenced-dependency", "1.0.1")));
    assertTrue(manifest.get(2).equals(ModuleId.create("org.foo.bar", "deep-referenced-dependency", "1.0.2")));
    assertTrue(manifest.get(3).equals(ModuleId.create("org.foo.bar", "direct-dependency", "1.0.0")));

    id = ModuleId.create("org.foo.bar", "with-deep-referenced-dependencies", "1.0.0");
    module = modules.get(id);
    assertNotNull(module);
    manifest = module.dependencies();
    assertFalse(manifest.isEmpty());
    assertEquals(4, manifest.size());
    assertTrue(manifest.get(0).equals(ModuleId.create("org.foo.bar", "deep-referenced-dependency", "1.0.0")));
    assertTrue(manifest.get(1).equals(ModuleId.create("org.foo.bar", "deep-referenced-dependency", "1.0.1")));
    assertTrue(manifest.get(2).equals(ModuleId.create("org.foo.bar", "deep-referenced-dependency", "1.0.2")));
    assertTrue(manifest.get(3).equals(ModuleId.create("org.foo.bar", "direct-dependency", "1.0.0")));

    id = ModuleId.create("org.foo.bar", "with-direct-deep-and-shallow-referenced-dependencies", "1.0.0");
    module = modules.get(id);
    assertNotNull(module);
    manifest = module.dependencies();
    assertFalse(manifest.isEmpty());
    assertEquals(7, manifest.size());
    assertTrue(manifest.get(0).equals(ModuleId.create("org.foo.bar", "deep-referenced-dependency", "1.0.0")));
    assertTrue(manifest.get(1).equals(ModuleId.create("org.foo.bar", "deep-referenced-dependency", "1.0.1")));
    assertTrue(manifest.get(2).equals(ModuleId.create("org.foo.bar", "deep-referenced-dependency", "1.0.2")));
    assertTrue(manifest.get(3).equals(ModuleId.create("org.foo.bar", "direct-dependency", "1.0.0")));
    assertTrue(manifest.get(4).equals(ModuleId.create("org.foo.bar", "referenced-dependency", "1.0.0")));
    assertTrue(manifest.get(5).equals(ModuleId.create("org.foo.bar", "referenced-dependency", "1.0.1")));
    assertTrue(manifest.get(6).equals(ModuleId.create("org.foo.bar", "referenced-dependency", "1.0.2")));
  }
  
  public void testIsOlder() {
    Modules modules = getModules("2.5.4", "/testList.xml");
    assertNotNull(modules);
    assertFalse(modules.list().isEmpty());
    assertEquals(4, modules.list().size());

    ModuleId id = ModuleId.create("org.foo.bar", "tim-apache-struts-1.1", "1.0.1");
    Module module = modules.get(id);
    assertNotNull(module);
    List<Module> siblings = module.getSiblings();
    assertNotNull(siblings);
    assertFalse(siblings.isEmpty());
    assertEquals(2, siblings.size());
    assertTrue(module.isOlder(siblings.get(0)));
    assertTrue(module.isOlder(siblings.get(1)));
    assertFalse(siblings.get(0).isLatest());
    assertTrue(siblings.get(1).isLatest());
  }
  
  public void testGetSiblings() {
    Modules modules = getModules("2.5.4", "/testList.xml");
    assertNotNull(modules);
    assertFalse(modules.list().isEmpty());
    assertEquals(4, modules.list().size());

    ModuleId id = ModuleId.create("org.foo.bar", "tim-annotations", "1.0.3");
    Module module = modules.get(id);
    assertNotNull(module);
    List<Module> siblings = module.getSiblings();
    assertNotNull(siblings);
    assertTrue(siblings.isEmpty());

    id = ModuleId.create("org.foo.bar", "tim-apache-struts-1.1", "1.0.1");
    module = modules.get(id);
    assertNotNull(module);
    siblings = module.getSiblings();
    assertNotNull(siblings);
    assertFalse(siblings.isEmpty());
    assertEquals(2, siblings.size());
    for (Module sibling : siblings) {
      String symname = ModuleId.computeSymbolicName("org.foo.bar", "tim-apache-struts-1.1");
      assertTrue(sibling.getSymbolicName().equals(symname));
      assertTrue(sibling.isSibling(module));
    }
  }

  public void testGetVersions() {
    Modules modules = getModules("2.5.0", "/testList.xml");
    assertNotNull(modules);
    assertFalse(modules.list().isEmpty());
    assertEquals(2, modules.list().size());

    ModuleId id = ModuleId.create("org.foo.bar", "tim-annotations", "1.0.0");
    Module module = modules.get(id);
    assertNotNull(module);
    List<String> versions = module.getVersions();
    assertNotNull(versions);
    assertEquals(1, versions.size());
    assertTrue(versions.get(0).equals("1.0.1"));

    id = ModuleId.create("org.foo.bar", "tim-annotations", "1.0.1");
    module = modules.get(id);
    assertNotNull(module);
    versions = module.getVersions();
    assertNotNull(versions);
    assertEquals(1, versions.size());
    assertTrue(versions.get(0).equals("1.0.0"));

    modules = getModules("2.5.4", "/testList.xml");
    assertNotNull(modules);
    assertFalse(modules.list().isEmpty());
    assertEquals(4, modules.list().size());

    id = ModuleId.create("org.foo.bar", "tim-annotations", "1.0.3");
    module = modules.get(id);
    assertNotNull(module);
    versions = module.getVersions();
    assertNotNull(versions);
    assertTrue(versions.isEmpty());

    id = ModuleId.create("org.foo.bar", "tim-apache-struts-1.1", "1.0.1");
    module = modules.get(id);
    assertNotNull(module);
    versions = module.getVersions();
    assertNotNull(versions);
    assertEquals(2, versions.size());
    assertTrue(versions.get(0).equals("1.0.2"));
    assertTrue(versions.get(1).equals("1.0.3"));

    id = ModuleId.create("org.foo.bar", "tim-apache-struts-1.1", "1.0.2");
    module = modules.get(id);
    assertNotNull(module);
    versions = module.getVersions();
    assertNotNull(versions);
    assertEquals(2, versions.size());
    assertTrue(versions.get(0).equals("1.0.1"));
    assertTrue(versions.get(1).equals("1.0.3"));

    id = ModuleId.create("org.foo.bar", "tim-apache-struts-1.1", "1.0.3");
    module = modules.get(id);
    assertNotNull(module);
    versions = module.getVersions();
    assertNotNull(versions);
    assertEquals(2, versions.size());
    assertTrue(versions.get(0).equals("1.0.1"));
    assertTrue(versions.get(1).equals("1.0.2"));
  }

  private Modules getModules(String tcversion, String file) {
    try {
      InputStream data = this.getClass().getResourceAsStream(file);
      assertNotNull(data);
      return new CachedModules(tcversion, new DataLoader(new File(file)));
    } catch (Exception e) {
      return null;
    }
  }
}
