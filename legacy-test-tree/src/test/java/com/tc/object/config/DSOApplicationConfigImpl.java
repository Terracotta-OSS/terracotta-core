/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import com.tc.config.schema.builder.ConfigBuilderFactory;
import com.tc.config.schema.builder.DSOApplicationConfigBuilder;
import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.LockConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;

import java.util.LinkedList;
import java.util.List;

/**
 * This class is an adapter between the ConfigVisitor class and the config builder stuff. It allows config to be
 * accreted during the visitation process and then baked into a config builder. I'm not extremely satisfied with the
 * naming and package structure of this thing, but it'll do. If you don't like it, feel free to move it.
 * 
 * @author orion
 */
public class DSOApplicationConfigImpl implements DSOApplicationConfig {

  private final ConfigBuilderFactory factory;
  private final List                 roots               = new LinkedList();
  private final List                 instrumentedClasses = new LinkedList();
  private final List                 locks               = new LinkedList();

  public DSOApplicationConfigImpl(ConfigBuilderFactory factory) {
    this.factory = factory;
  }

  public void writeTo(DSOApplicationConfigBuilder builder) {
    if (!roots.isEmpty()) {
      builder.setRoots((RootConfigBuilder[]) roots.toArray(new RootConfigBuilder[roots.size()]));
    }
    builder.setInstrumentedClasses((InstrumentedClassConfigBuilder[]) instrumentedClasses
        .toArray(new InstrumentedClassConfigBuilder[instrumentedClasses.size()]));
    builder.setLocks((LockConfigBuilder[]) locks.toArray(new LockConfigBuilder[locks.size()]));
  }

  public void addRoot(String rootName, String rootFieldName) {
    RootConfigBuilder root = factory.newRootConfigBuilder();
    root.setRootName(rootName);
    root.setFieldName(rootFieldName);
    roots.add(root);
  }

  public void addIncludePattern(String classPattern) {
    addIncludePattern(classPattern, false);
  }

  public void addIncludePattern(String classPattern, boolean honorTransient) {
    InstrumentedClassConfigBuilder instrumentedClass = factory.newInstrumentedClassConfigBuilder();
    instrumentedClass.setClassExpression(classPattern);
    instrumentedClass.setHonorTransient(honorTransient);
    addInstrumentedClass(instrumentedClass);
  }

  private void addInstrumentedClass(InstrumentedClassConfigBuilder instrumentedClass) {
    instrumentedClasses.add(instrumentedClass);
  }

  public void addWriteAutolock(String methodPattern) {
    LockConfigBuilder lock = factory.newWriteAutoLockConfigBuilder();
    lock.setMethodExpression(methodPattern);
    locks.add(lock);
  }
  
  public void addReadAutolock(String methodPattern) {
    LockConfigBuilder lock = factory.newReadAutoLockConfigBuilder();
    lock.setMethodExpression(methodPattern);
    locks.add(lock);
  }

}
