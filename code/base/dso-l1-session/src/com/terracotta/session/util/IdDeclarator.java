/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session.util;


/**
 * generates session cookie id of the format: <4hex:random><4hex:nextId>[[16hex:random]*]
 */
public interface IdDeclarator {
  String transform(String sessionId);
}
