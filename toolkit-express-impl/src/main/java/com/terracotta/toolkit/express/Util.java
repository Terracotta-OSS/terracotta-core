/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express;


class Util {

  private Util() {
    //
  }

  static <T> T getImplInstance(String type) {
    try {
      return (T) Class.forName(type).newInstance();
    } catch (Exception e) {
      if (e instanceof RuntimeException) { throw (RuntimeException) e; }
      throw new RuntimeException(e);
    }
  }

}