/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.lang;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;

public class EqualsBuilderTracer extends TCEqualsBuilder {

  private static final TCLogger logger = TCLogging.getLogger(EqualsBuilderTracer.class);

  private TCEqualsBuilder       delegate;

  public EqualsBuilderTracer() {
    delegate = new TCEqualsBuilder();
  }

  public EqualsBuilderTracer(TCEqualsBuilder v) {
    Assert.assertNotNull(v);
    delegate = v;
  }

  public TCEqualsBuilder append(boolean arg0, boolean arg1) {
    delegate = delegate.append(arg0, arg1);
    if (logger.isDebugEnabled()) checkAndLog("boolean", String.valueOf(arg0), String.valueOf(arg1));
    return this;
  }

  public TCEqualsBuilder append(boolean[] arg0, boolean[] arg1) {
    delegate = delegate.append(arg0, arg1);
    if (logger.isDebugEnabled()) checkAndLog("boolean[]", String.valueOf(arg0), String.valueOf(arg1));
    return this;
  }

  public TCEqualsBuilder append(byte arg0, byte arg1) {
    delegate = delegate.append(arg0, arg1);
    if (logger.isDebugEnabled()) checkAndLog("byte", String.valueOf(arg0), String.valueOf(arg1));
    return this;
  }

  public TCEqualsBuilder append(byte[] arg0, byte[] arg1) {
    delegate = delegate.append(arg0, arg1);
    if (logger.isDebugEnabled()) checkAndLog("byte[]", String.valueOf(arg0), String.valueOf(arg1));
    return this;
  }

  public TCEqualsBuilder append(char arg0, char arg1) {
    delegate = delegate.append(arg0, arg1);
    if (logger.isDebugEnabled()) checkAndLog("char", String.valueOf(arg0), String.valueOf(arg1));
    return this;
  }

  public TCEqualsBuilder append(char[] arg0, char[] arg1) {
    delegate = delegate.append(arg0, arg1);
    if (logger.isDebugEnabled()) checkAndLog("char[]", String.valueOf(arg0), String.valueOf(arg1));
    return this;
  }

  public TCEqualsBuilder append(double arg0, double arg1) {
    delegate = delegate.append(arg0, arg1);
    if (logger.isDebugEnabled()) checkAndLog("double", String.valueOf(arg0), String.valueOf(arg1));
    return this;
  }

  public TCEqualsBuilder append(double[] arg0, double[] arg1) {
    delegate = delegate.append(arg0, arg1);
    if (logger.isDebugEnabled()) checkAndLog("double[]", String.valueOf(arg0), String.valueOf(arg1));
    return this;
  }

  public TCEqualsBuilder append(float arg0, float arg1) {
    delegate = delegate.append(arg0, arg1);
    if (logger.isDebugEnabled()) checkAndLog("float", String.valueOf(arg0), String.valueOf(arg1));
    return this;
  }

  public TCEqualsBuilder append(float[] arg0, float[] arg1) {
    delegate = delegate.append(arg0, arg1);
    if (logger.isDebugEnabled()) checkAndLog("float[]", String.valueOf(arg0), String.valueOf(arg1));
    return this;
  }

  public TCEqualsBuilder append(int arg0, int arg1) {
    delegate = delegate.append(arg0, arg1);
    if (logger.isDebugEnabled()) checkAndLog("int", String.valueOf(arg0), String.valueOf(arg1));
    return this;
  }

  public TCEqualsBuilder append(int[] arg0, int[] arg1) {
    delegate = delegate.append(arg0, arg1);
    if (logger.isDebugEnabled()) checkAndLog("int[]", String.valueOf(arg0), String.valueOf(arg1));
    return this;
  }

  public TCEqualsBuilder append(long arg0, long arg1) {
    delegate = delegate.append(arg0, arg1);
    if (logger.isDebugEnabled()) checkAndLog("long", String.valueOf(arg0), String.valueOf(arg1));
    return this;
  }

  public TCEqualsBuilder append(long[] arg0, long[] arg1) {
    delegate = delegate.append(arg0, arg1);
    if (logger.isDebugEnabled()) checkAndLog("long[]", String.valueOf(arg0), String.valueOf(arg1));
    return this;
  }

  public TCEqualsBuilder append(Object arg0, Object arg1) {
    delegate = delegate.append(arg0, arg1);
    if (logger.isDebugEnabled()) checkAndLog(getTypeName(arg0, arg1), String.valueOf(arg0), String.valueOf(arg1));
    return this;
  }

  public TCEqualsBuilder append(Object[] arg0, Object[] arg1) {
    delegate = delegate.append(arg0, arg1);
    if (logger.isDebugEnabled()) checkAndLog(getTypeName(arg0, arg1), String.valueOf(arg0), String.valueOf(arg1));
    return this;
  }

  public TCEqualsBuilder append(short arg0, short arg1) {
    delegate = delegate.append(arg0, arg1);
    if (logger.isDebugEnabled()) checkAndLog("short", String.valueOf(arg0), String.valueOf(arg1));
    return this;
  }

  public TCEqualsBuilder append(short[] arg0, short[] arg1) {
    delegate = delegate.append(arg0, arg1);
    if (logger.isDebugEnabled()) checkAndLog("short[]", String.valueOf(arg0), String.valueOf(arg1));
    return this;
  }

  public TCEqualsBuilder appendSuper(boolean arg0) {
    delegate = delegate.appendSuper(arg0);
    if (logger.isDebugEnabled()) checkAndLog("super", "<n/a>", "<n/a>");
    return this;
  }

  public boolean isEquals() {
    return delegate.isEquals();
  }

  private void checkAndLog(String type, String val1, String val2) {
    if (!delegate.isEquals()) {
      logger.debug("Objects not equal: type = " + type + ", val1=" + val1 + ", val2=" + val2,
                   new Throwable("StackTrace"));
    }
  }

  private String getTypeName(Object o) {
    if (o == null) {
      return "<null>";
    } else {
      return o.getClass().getName();
    }
  }

  private String getTypeName(Object o1, Object o2) {
    return getTypeName(o1) + "/" + getTypeName(o2);
  }

}