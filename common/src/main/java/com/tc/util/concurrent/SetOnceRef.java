/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.util.concurrent;

/**
 * An object reference holder class in which the reference can only be set once. This might be useful for a member
 * variable that you'd like to make final, but that you aren't able to set within a constructor. I suppose this class is
 * a lot like TCFuture, but this class doesn't offer methods to wait for a value to set. The use case for this class has
 * nothing to do with communication/synchronization between threads. This class is meant to make sure you can't change a
 * reference once it's been set (ie. it provides a reference member variable that behaves like it was marked "final" and
 * won't let anyone read it until it has been set)
 * 
 * @author teck
 */
public final class SetOnceRef<V> {
  private V             ref;
  private boolean       set;
  private final boolean allowsNullValue;

  /**
   * Create a new <code>set()</code> 'able instance This instance <b>will not </b> permitted to contain a null
   * reference
   */
  public SetOnceRef() {
    this(null, false, false);
  }

  /**
   * Create and immediately <code>set()</code> to the given reference. This instance <b>will not </b> permitted to
   * contain a null reference
   * 
   * @param ref The reference to hold
   * @throws IllegalArgumentException If the reference to hold is null
   */
  public SetOnceRef(V ref) {
    this(ref, false, true);
  }

  /**
   * Create and immediately <code>set()</code> to the given refernce
   * 
   * @param ref the reference to hold
   * @param allowNull true to allow nulls to be stored in this instance
   * @throws IllegalArgumentException if allowNull is true and the given reference is null
   */
  public SetOnceRef(V ref, boolean allowNull) {
    this(ref, allowNull, true);
  }

  /**
   * Create a new <code>set()</code> 'able instance
   * 
   * @param allowNull true to allow this instance to hold the value null
   */
  public SetOnceRef(boolean allowNull) {
    this(null, allowNull, false);
  }

  private SetOnceRef(V ref, boolean allowNull, boolean init) {
    this.allowsNullValue = allowNull;

    if (init) {
      set(ref);
    }
  }

  /**
   * Can null ever be the value of this reference
   * 
   * @return true if null is allowed, false otherwise
   */
  public boolean allowsNull() {
    return this.allowsNullValue;
  }

  /**
   * Attempt to set the reference to the given value
   * 
   * @param ref the reference to set in this instance
   * @throws IllegalStateException if the reference has already been set by another thread
   * @throws IllegalArgumentException if the given reference is null and this instance does not allow the null value
   */
  public synchronized void set(V ref) {
      if (set) { throw new IllegalStateException("Reference has already been set"); }

      if ((!allowsNull()) && (ref == null)) { throw new IllegalArgumentException(
                                                                                 "This instance cannot hold a null reference value"); }

      set = true;
      this.ref = ref;
  }

  /**
   * Get the reference value contained in this instance
   * 
   * @return the reference, may be null if this instance allow nulls (see <code>allowNull</code>
   * @throws IllegalStateException if a valid reference value has not yet been set
   */
  public synchronized V get() {
      if (!set) { throw new IllegalStateException("Reference has not been set"); }

      return ref;
  }

  /**
   * Has someone set the reference yet
   * 
   * @return true iff the reference has been set
   */
  public synchronized boolean isSet() {
      return set;
  }
}
