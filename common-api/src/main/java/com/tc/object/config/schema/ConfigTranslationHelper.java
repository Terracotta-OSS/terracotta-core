/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config.schema;

import org.apache.xmlbeans.XmlObject;

import com.tc.util.Assert;
import com.terracottatech.config.Autolock;
import com.terracottatech.config.ClassExpression;
import com.terracottatech.config.Include;
import com.terracottatech.config.InstrumentedClasses;
import com.terracottatech.config.LockLevel;
import com.terracottatech.config.Locks;
import com.terracottatech.config.NamedLock;
import com.terracottatech.config.OnLoad;

/**
 * Knows how to translate various chunks of config between their XMLBeans representations and the internal
 * representations we actually use.
 */
public class ConfigTranslationHelper {

  static InstrumentedClass[] translateIncludes(InstrumentedClasses instrumentedClasses) {
    XmlObject[] objects = instrumentedClasses.selectPath("*");
    InstrumentedClass[] classes = new InstrumentedClass[objects.length];

    Assert.eval(classes.length == objects.length);
    for (int i = 0; i < objects.length; ++i) {
      if (objects[i] instanceof Include) {
        Include theInclude = (Include) objects[i];

        // XXX: "honor volatile" should probably be exposed in config
        boolean honorVolatile = false;

        classes[i] = new IncludedInstrumentedClass(theInclude.getClassExpression(), theInclude.getHonorTransient(),
                                                   honorVolatile, ConfigTranslationHelper.createOnLoadObj(theInclude
                                                       .getOnLoad()));
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

  static Lock[] translateLocks(Locks locks) {
    NamedLock[] namedLocks = locks.getNamedLockArray();
    Autolock[] autoLocks = locks.getAutolockArray();

    int namedLength = namedLocks == null ? 0 : namedLocks.length;
    int autoLength = autoLocks == null ? 0 : autoLocks.length;

    Lock[] out = new Lock[namedLength + autoLength];
    for (int i = 0; i < namedLength; ++i) {
      com.tc.object.config.schema.LockLevel level;

      if (namedLocks[i].getLockLevel() != null) {
        if (namedLocks[i].getLockLevel().equals(LockLevel.CONCURRENT)) level = com.tc.object.config.schema.LockLevel.CONCURRENT;
        else if (namedLocks[i].getLockLevel().equals(LockLevel.READ)) level = com.tc.object.config.schema.LockLevel.READ;
        else if (namedLocks[i].getLockLevel().equals(LockLevel.WRITE)) level = com.tc.object.config.schema.LockLevel.WRITE;
        else if (namedLocks[i].getLockLevel().equals(LockLevel.SYNCHRONOUS_WRITE)) level = com.tc.object.config.schema.LockLevel.SYNCHRONOUS_WRITE;
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
        else if (autoLocks[i].getLockLevel().equals(LockLevel.SYNCHRONOUS_WRITE)) level = com.tc.object.config.schema.LockLevel.SYNCHRONOUS_WRITE;
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
