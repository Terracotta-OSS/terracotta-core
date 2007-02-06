/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.schema;

import org.apache.xmlbeans.XmlObject;

import com.tc.util.Assert;
import com.terracottatech.configV2.Autolock;
import com.terracottatech.configV2.ClassExpression;
import com.terracottatech.configV2.Include;
import com.terracottatech.configV2.LockLevel;
import com.terracottatech.configV2.Locks;
import com.terracottatech.configV2.NamedLock;
import com.terracottatech.configV2.OnLoad;

/**
 * Knows how to translate various chunks of config between their XMLBeans representations and the internal
 * representations we actually use.
 */
public class ConfigTranslationHelper {

  static Object translateIncludes(XmlObject xmlObject) {
    if (xmlObject == null) return null;
  
    XmlObject[] objects = xmlObject.selectPath("*");
    InstrumentedClass[] classes = new InstrumentedClass[objects.length];
  
    Assert.eval(classes.length == objects.length);
    for (int i = 0; i < objects.length; ++i) {
      if (objects[i] instanceof Include) {
        Include theInclude = (Include) objects[i];
  
        // XXX: "honor volatile" should probably be exposed in config
        boolean honorVolatile = false;
  
        classes[i] = new IncludedInstrumentedClass(theInclude.getClassExpression(), theInclude.getHonorTransient(),
                                                   honorVolatile, ConfigTranslationHelper.createOnLoadObj(theInclude.getOnLoad()));
      } else if (objects[i] instanceof ClassExpression) {
        ClassExpression theExpression = (ClassExpression) objects[i];
        classes[i] = new ExcludedInstrumentedClass(theExpression.getStringValue());
      } else {
        throw Assert.failure("Child #" + i + " of the <instrumented-classes> element appears to be "
                             + "neither an <include> nor an <exclude> element. This is a programming error in "
                             + "Terracotta software.");
      }
    }
  
    return classes;
  }

  static Object translateLocks(XmlObject xmlObject) {
    if (xmlObject == null) return null;
  
    NamedLock[] namedLocks = ((Locks) xmlObject).getNamedLockArray();
    Autolock[] autoLocks = ((Locks) xmlObject).getAutolockArray();
  
    int namedLength = namedLocks == null ? 0 : namedLocks.length;
    int autoLength = autoLocks == null ? 0 : autoLocks.length;
  
    Lock[] out = new Lock[namedLength + autoLength];
    for (int i = 0; i < namedLength; ++i) {
      com.tc.object.config.schema.LockLevel level;
  
      if (namedLocks[i].getLockLevel() != null) {
        if (namedLocks[i].getLockLevel().equals(LockLevel.CONCURRENT)) level = com.tc.object.config.schema.LockLevel.CONCURRENT;
        else if (namedLocks[i].getLockLevel().equals(LockLevel.READ)) level = com.tc.object.config.schema.LockLevel.READ;
        else if (namedLocks[i].getLockLevel().equals(LockLevel.WRITE)) level = com.tc.object.config.schema.LockLevel.WRITE;
        else throw Assert.failure("Unknown lock level " + namedLocks[i].getLockLevel());
      } else {
        level = com.tc.object.config.schema.LockLevel.WRITE;
      }
  
      out[i] = new com.tc.object.config.schema.NamedLock(namedLocks[i].getLockName(), namedLocks[i]
          .getMethodExpression(), level);
    }
  
    for (int i = 0; i < autoLength; ++i) {
      com.tc.object.config.schema.LockLevel level;
  
      if (autoLocks[i].getLockLevel() != null) {
        if (autoLocks[i].getLockLevel().equals(LockLevel.CONCURRENT)) level = com.tc.object.config.schema.LockLevel.CONCURRENT;
        else if (autoLocks[i].getLockLevel().equals(LockLevel.READ)) level = com.tc.object.config.schema.LockLevel.READ;
        else if (autoLocks[i].getLockLevel().equals(LockLevel.WRITE)) level = com.tc.object.config.schema.LockLevel.WRITE;
        else throw Assert.failure("Unknown lock level " + namedLocks[i].getLockLevel());
      } else {
        level = com.tc.object.config.schema.LockLevel.WRITE;
      }
  
      out[namedLength + i] = new com.tc.object.config.schema.AutoLock(autoLocks[i].getMethodExpression(), level);
    }
  
    return out;
  }

  private static IncludeOnLoad createOnLoadObj(OnLoad xmlOnLoad) {
    if (xmlOnLoad == null) return new IncludeOnLoad();
  
    Object value;
    if ((value = xmlOnLoad.getExecute()) != null) return new IncludeOnLoad(IncludeOnLoad.EXECUTE, value);
    if ((value = xmlOnLoad.getMethod()) != null) return new IncludeOnLoad(IncludeOnLoad.METHOD, value);
  
    return new IncludeOnLoad();
  }

  
  
}
