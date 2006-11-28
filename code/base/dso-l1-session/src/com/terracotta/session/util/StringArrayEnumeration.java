/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.terracotta.session.util;

import java.util.Enumeration;
import java.util.NoSuchElementException;

public class StringArrayEnumeration implements Enumeration {

  private static final String[] EMPTY_ARRAY = new String[0];
  private final String[]        strings;
  private int                   currIndex   = 0;

  public StringArrayEnumeration(String[] strings) {
    this.strings = (strings == null) ? EMPTY_ARRAY : strings;
  }

  public boolean hasMoreElements() {
    return currIndex < strings.length;
  }

  public Object nextElement() {
    if (currIndex >= strings.length) throw new NoSuchElementException();
    String rv = strings[currIndex];
    currIndex++;
    return rv;
  }

}
