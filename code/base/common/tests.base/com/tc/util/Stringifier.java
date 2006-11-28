/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.util;

/**
 * Knows how to turn objects into strings in some fashion. This object must be able to accept <code>null</code> as its
 * argument.
 */
public interface Stringifier {

  String toString(Object o);

}