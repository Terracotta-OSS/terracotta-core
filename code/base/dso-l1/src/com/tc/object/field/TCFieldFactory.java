/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.field;

import com.tc.object.TCClass;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author orion
 */
public class TCFieldFactory {
  private DSOClientConfigHelper configuration;

  public TCFieldFactory(DSOClientConfigHelper configuration) {
    this.configuration = configuration;
  }

  public TCField getInstance(TCClass tcClass, Field field) {
    TransparencyClassSpec spec = configuration.getSpec(tcClass.getName());
    boolean trans = false;
    if (spec != null) {
      trans = spec.isTransient(field.getModifiers(), field.getName());
    }
    return new GenericTCField(tcClass, field, !Modifier.isStatic(field.getModifiers()) && !trans);
  }

}