/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject.bytecode;

import com.tc.objectserver.managedobject.PhysicalManagedObjectState;
import com.tc.util.Assert;

public class ClassSpec {

  private static final String PHYSICAL_MO_STATE_CLASS_NAME = PhysicalManagedObjectState.class.getName();

  private final String        className;
  private final String        loaderDesc;
  private final String        classIdentifier;
  private boolean             generateParentIdStorage;
  private int                 classID                      = Integer.MIN_VALUE;
  private String              generatedClassName;
  private String              superClassName               = PHYSICAL_MO_STATE_CLASS_NAME;

  public ClassSpec(String className, String loaderDesc, long strIdx) {
    this.className = className;
    this.loaderDesc = loaderDesc;
    this.classIdentifier = "com.tc.state.idx" + strIdx + "." + className;
  }

  public void setGeneratedClassID(int classID) {
    this.classID = classID;
    this.generatedClassName = this.classIdentifier + "_V" + classID;
  }

  public String getGeneratedClassName() {
    Assert.assertNotNull(this.generatedClassName);
    return this.generatedClassName;
  }

  public String getClassName() {
    return className;
  }

  public String getLoaderDesc() {
    return loaderDesc;
  }

  public String getClassIdentifier() {
    return classIdentifier;
  }

  public int getClassID() {
    Assert.assertFalse(this.classID == Integer.MIN_VALUE);
    return this.classID;
  }

  public Object getLock() {
    return classIdentifier.intern();
  }

  public void setSuperClassName(String className) {
    this.superClassName = className;
  }

  public String getSuperClassName() {
    return this.superClassName;
  }

  public boolean isDirectSubClassOfPhysicalMOState() {
    return (PHYSICAL_MO_STATE_CLASS_NAME.equals(this.superClassName));
  }

  public void setGenerateParentIdStorage(boolean b) {
    this.generateParentIdStorage = b;
  }

  public boolean generateParentIdStorage() {
    return this.generateParentIdStorage;
  }
}
