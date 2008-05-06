/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.diff;

import org.apache.commons.lang.builder.EqualsBuilder;

import com.tc.util.Assert;
import com.tc.util.StandardStringifier;
import com.tc.util.Stringifier;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Passed along among {@link Differenceable}objects in order to display where the differences are in an object tree.
 */
public class DifferenceContext {

  private final DifferenceContext previous;
  private final String            thisContext;
  private final List              differences;
  private final Stringifier       stringifier;

  private DifferenceContext(DifferenceContext previous, String thisContext) {
    Assert.assertNotNull(previous);
    Assert.assertNotBlank(thisContext);

    this.previous = previous;
    this.thisContext = thisContext;
    this.differences = this.previous.differences;
    this.stringifier = this.previous.stringifier;
  }

  public DifferenceContext(Stringifier stringifier) {
    Assert.assertNotNull(stringifier);
    
    this.previous = null;
    this.thisContext = "";
    this.differences = new LinkedList();
    this.stringifier = stringifier;
  }

  public static DifferenceContext createInitial() {
    return createInitial(StandardStringifier.INSTANCE);
  }

  public static DifferenceContext createInitial(Stringifier stringifier) {
    return new DifferenceContext(stringifier);
  }

  public DifferenceContext sub(String context) {
    return new DifferenceContext(this, context);
  }

  /**
   * For <strong>TESTS ONLY </strong>.
   */
  Collection collection() {
    return this.differences;
  }

  Stringifier stringifier() {
    return this.stringifier;
  }
  
  String describe(Object o) {
    return this.stringifier.toString(o);
  }

  void addDifference(Difference difference) {
    Assert.assertNotNull(difference);
    Assert.eval(difference.where() == this);
    this.differences.add(difference);
  }

  Iterator getDifferences() {
    return this.differences.iterator();
  }

  boolean hasDifferences() {
    return this.differences.size() > 0;
  }

  /**
   * For <strong>TESTS ONLY </strong>.
   */
  int countDifferences() {
    return this.differences.size();
  }

  public String toString() {
    if (this.previous != null) return this.previous.toString() + "/" + this.thisContext;
    else return this.thisContext;
  }

  public boolean equals(Object that) {
    if (!this.rawEquals(that)) return false;

    return new EqualsBuilder().append(this.differences, ((DifferenceContext) that).differences).isEquals();
  }

  boolean rawEquals(Object that) {
    if (!(that instanceof DifferenceContext)) return false;

    DifferenceContext diffThat = (DifferenceContext) that;

    if ((this.previous == null) != (diffThat.previous == null)) return false;
    if (this.previous != null && (!this.previous.rawEquals(diffThat.previous))) return false;

    return new EqualsBuilder().append(this.thisContext, diffThat.thisContext).isEquals();
  }

}