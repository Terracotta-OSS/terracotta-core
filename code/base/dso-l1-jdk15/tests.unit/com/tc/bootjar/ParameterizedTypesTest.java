/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bootjar;

import com.tc.asm.ClassReader;
import com.tc.asm.tree.ClassNode;
import com.tc.asm.tree.MethodNode;
import com.tc.object.tools.BootJar;
import com.tc.test.TCTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class ParameterizedTypesTest extends TCTestCase {
  
  public void testParameterizedTypesTest() throws Exception {
    BootJar bj = BootJar.getDefaultBootJarForReading();
    Set specs = bj.getAllPreInstrumentedClasses();
    for (Iterator iter = specs.iterator(); iter.hasNext();) {
      String className = (String) iter.next();
      checkParmeterizedType(className);
    }
    
  }

  private void checkParmeterizedType(String className) throws Exception {
    Class klass = Class.forName(className);
    Type gsc = klass.getGenericSuperclass();
    System.err.println("GenericSuperClass for " + className + " is " + gsc);
  }
  
  public void testHashMap() throws Exception {
    assertMethodSignatures("java/util/HashMap.class");
  }

  public void testHashtable() throws Exception {
    assertMethodSignatures("java/util/Hashtable.class");
  }
  
  public void testLinkedHashMap() throws Exception {
    assertMethodSignatures("java/util/LinkedHashMap.class");
  }
  
  private void assertMethodSignatures(String className) throws IOException {
    ClassNode jcn = getOriginalClass(className);
    ClassNode tcn = getCurrentClass(className);
    
    for (Iterator it = tcn.methods.iterator(); it.hasNext();) {
      MethodNode tnode = (MethodNode) it.next();
      MethodNode jnode = findMethod(jcn.methods, tnode);
      if(jnode!=null) {
        assertEquals("Invalid signature for " + tnode.name + tnode.desc, jnode.signature, tnode.signature);
      }
    }
  }

  private MethodNode findMethod(List methods, MethodNode jnode) {
    for (Iterator it = methods.iterator(); it.hasNext();) {
      MethodNode tnode = (MethodNode) it.next();
      if(tnode.name.equals(jnode.name) && tnode.desc.equals(jnode.desc)) {
        return tnode;
      }
    }
    return null;
  }

  private ClassNode getCurrentClass(String className) throws IOException {
    InputStream is = getClass().getResourceAsStream("/" + className);
    ClassReader cr = new ClassReader(is);
    ClassNode cn = new ClassNode();
    cr.accept(cn, 0);
    return cn;
  }

  private ClassNode getOriginalClass(String className) throws IOException {
    // original rt.jar - jar:file:/C:/jdk1.5.0_08/jre/lib/rt.jar!/java/lang/Void.class
    URL resource = Void.class.getResource("/java/lang/Void.class");
    assertNotNull("Unable to find original rt.jar", resource);
    
    String path = resource.toString();
    String jarPath = path.substring("jar:file:".length(), path.indexOf('!'));
    JarFile jarFile = new JarFile(jarPath);
    ZipEntry entry = jarFile.getEntry(className);
    InputStream is = jarFile.getInputStream(entry);
    assertNotNull("Unable to find zip entry " + className, is);
    
    ClassReader cr = new ClassReader(is);
    ClassNode cn = new ClassNode();
    cr.accept(cn, 0);
    return cn;
  }

}
