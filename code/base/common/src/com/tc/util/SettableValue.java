/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.Serializable;

/**
 * A simple value (of any Object type) that can be in three states, not just two:
 * <ul>
 * <li>Unset.</li>
 * <li>Set, but null.</li>
 * <li>Set, with a value.</li>
 * </ul>
 * This lets us handle a myriad of cases in the config where we need to distinguish between something having never
 * been set and something having been set explicitly to null.
 */
public class SettableValue implements Serializable {
  
  private Object value;
  private boolean isSet;
  
  public SettableValue() {
    this.value = null;
    this.isSet = false;
  }
  
  public void set(Object value) {
    this.value = value;
    this.isSet = true;
  }
  
  public void unset() {
    this.value = null;
    this.isSet = false;
  }
  
  public boolean isSet() {
    return this.isSet;
  }
  
  public Object value() {
    return this.value;
  }
  
  /**
   * @returns defaultValue if value has not been set
   */
  public Object value(Object defaultValue) {
    if (this.isSet) {
      return this.value;
    } else {
      return defaultValue;
    }
  }
  
  public boolean equals(Object that) {
    if (that == this) return true;
    if (that == null) return false;
    if (! (that instanceof SettableValue)) return false;
    
    SettableValue valueThat = (SettableValue) that;
    if (this.isSet != valueThat.isSet) return false;
    if ((this.value == null) != (valueThat.value == null)) return false;
    if (this.value != null) return this.value.equals(valueThat.value);
    else return true;
  }
  
  public int hashCode() {
    return new HashCodeBuilder().append(isSet).append(value).toHashCode();
  }
  
  public Object clone() {
    SettableValue out = new SettableValue();
    if (this.isSet) out.set(this.value);
    return out;
  }
  
  public String toString() {
    if (! isSet) return "<unset>";
    if (value == null) return "<null>";
    return value.toString();
  }

}