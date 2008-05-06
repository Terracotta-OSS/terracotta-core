/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

/**
 * Knows how to turn objects into strings in some fashion. This object must be able to accept <code>null</code> as its
 * argument.
 */
public interface Stringifier {

  String toString(Object o);

}