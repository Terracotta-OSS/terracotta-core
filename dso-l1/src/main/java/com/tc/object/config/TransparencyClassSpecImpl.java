/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

/**
 * Describe the Custom adaption of a class
 */
public class TransparencyClassSpecImpl implements TransparencyClassSpec {

  private final String               className;
  private final String               changeApplicatorClassName;
  private final ChangeApplicatorSpec changeApplicatorSpec;
  private boolean                    useNonDefaultConstructor = false;

  public TransparencyClassSpecImpl(final String className, final String changeApplicatorClassName) {
    this.className = className;
    this.changeApplicatorClassName = changeApplicatorClassName;
    this.changeApplicatorSpec = new DSOChangeApplicatorSpec(changeApplicatorClassName);
  }

  @Override
  public String getClassName() {
    return className;
  }

  @Override
  public ChangeApplicatorSpec getChangeApplicatorSpec() {
    return changeApplicatorSpec;
  }

  @Override
  public boolean isUseNonDefaultConstructor() {
    return this.useNonDefaultConstructor;
  }

  @Override
  public void setUseNonDefaultConstructor(final boolean useNonDefaultConstructor) {
    this.useNonDefaultConstructor = useNonDefaultConstructor;
  }

  @Override
  public String getChangeApplicatorClassName() {
    return this.changeApplicatorClassName;
  }

}
