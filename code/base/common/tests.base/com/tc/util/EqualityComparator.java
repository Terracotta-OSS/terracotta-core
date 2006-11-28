/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.util;

/**
 * Knows how to compare two objects for equality. This object must be able to accept <code>null</code> values for
 * either argument &mdash; <code>null</code> should never be equal to anything except itself.
 */
public interface EqualityComparator {

  boolean isEquals(Object one, Object two);

}