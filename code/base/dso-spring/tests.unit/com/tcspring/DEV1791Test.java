/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import com.tc.aspectwerkz.definition.deployer.StandardAspectModuleDeployer;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.aspectwerkz.transform.InstrumentationContext;
import com.tc.object.bytecode.hook.impl.DefaultWeavingStrategy;
import com.tc.object.tools.BootJarTool;

import junit.framework.TestCase;

/**
 * Tests that certain classes are matched as advisable, and others are not.  If a class is needlessly matched as advisable, it can
 * result in the aspectwerkz AW:: warnings that were seen in DEV1791, etc, because DefaultWeavingStrategy will attempt to eagerly load
 * such a class and all inner static classes.
 */
public class DEV1791Test extends TestCase {
  

  private ClassLoader classLoader;

  protected void setUp() throws Exception {
    super.setUp();
    classLoader = getClass().getClassLoader();
    
    //TODO refactor this static call
    StandardAspectModuleDeployer.deploy(classLoader, "com.tc.object.config.SpringAspectModule");
  }
  
  public void testSpringDemoClassIsAdvisable() throws Exception {
    
    helpTestClassIsAdvisable("org.springframework.core.CollectionFactory", false);
    helpTestClassIsAdvisable("org.springframework.context.support.AbstractApplicationContext", true);
    helpTestClassIsAdvisable("org.springframework.context.support.AbstractRefreshableApplicationContext", true);
    helpTestClassIsAdvisable("org.springframework.web.context.support.XmlWebApplicationContext", true);
    helpTestClassIsAdvisable("org.springframework.context.support.ClassPathXmlApplicationContext", true);
  }

  private void helpTestClassIsAdvisable(final String className, final boolean isExpectedToBeAdvisable) throws ClassNotFoundException {
    final byte[] classBytes = BootJarTool.getBytesForClass(className, classLoader);
    final InstrumentationContext context = new InstrumentationContext(className, classBytes, classLoader);
    final ClassInfo classInfo = AsmClassInfo.getClassInfo(className, context.getInitialBytecode(), classLoader);
    
    final boolean isAdvisable = DefaultWeavingStrategy.isAdvisable(classInfo, context.getDefinitions());
    assertEquals(isExpectedToBeAdvisable, isAdvisable);
  }

}
