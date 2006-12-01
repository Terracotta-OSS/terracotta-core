/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Represents configuration goo that hangs off of a particular class. Note: This class has no synchronization. If you
 * want to use it in a multi-threaded context, you should add it.
 */
public class Type {

  public static final int STATE_OPEN      = 1;
  public static final int STATE_COMMITTED = 2;

  private String          typeName;
  private final HashSet   transients      = new HashSet();
  private int             state           = STATE_OPEN;

  public Type() {
    //
  }

  public Type(String typeName, String[] transientNames) {
    this.typeName = typeName;
    addTransients(transientNames);
  }

  public String getName() {
    return this.typeName;
  }

  public void setName(String name) {
    writeTest();
    validateName(name);
    this.typeName = name;
  }

  private void validateName(String name) {
    if (name == null) {
      throw new IllegalArgumentException("Class name cannot be null");
    } else if ("".equals(name.trim())) { throw new IllegalArgumentException("Invalid class name: " + name); }
  }

  public void addTransients(String[] transientNames) {
    writeTest();
    if (transientNames != null) {
      if (transientNames != null) {
        for (int i = 0; i < transientNames.length; i++) {
          addTransient(transientNames[i]);
        }
      }
    }
  }

  public void addTransient(String transientName) {
    writeTest();
    validateTransient(transientName);
    this.transients.add(transientName);
  }

  private void validateTransient(String transientName) {
    if (transientName == null) {
      throw new IllegalArgumentException("transient may not be null.");
    } else if ("".equals(transientName.trim())) { throw new IllegalArgumentException("invalid transient: "
                                                                                     + transientName); }
  }

  public Set getTransients() {
    return (Set) this.transients.clone();
  }

  /**
   * XXX: It's a bit goofy to throw an IllegalArgumentException. It's done this way because the validation is defined in
   * terms of the field validator methods which throw IllegalArgumentExceptions. I didn't feel like creating a new
   * exception type or hunting around for a more appropriate one.
   */
  public void validate() throws IllegalArgumentException {
    validateName(getName());
    for (Iterator i = this.transients.iterator(); i.hasNext();) {
      validateTransient((String) i.next());
    }
  }

  public void commit() {
    this.state = STATE_COMMITTED;
  }

  private void writeTest() {
    if (this.state != STATE_OPEN) throw new IllegalStateException("Attempt to write to a committed Type object.");
  }

  public boolean containsTransient(String field) {
    if (field != null) { return this.transients.contains(field); }
    return false;
  }
  
  public String toString() {
    return getClass().getName() + "[typeName: " + typeName + ", transients: " + transients + "]";
  }
}