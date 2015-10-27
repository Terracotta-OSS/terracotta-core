/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.util;

import java.util.Iterator;
import java.util.ServiceLoader;

public class ServiceUtil {

  public static <T> T loadService(Class<T> c) {
    ServiceLoader<T> serviceLoader = ServiceLoader.load(c, ServiceUtil.class.getClassLoader());

    T rv = null;
    for (Iterator<T> iter = serviceLoader.iterator(); iter.hasNext();) {
      if (rv == null) {
        rv = iter.next();
      } else {
        throw new AssertionError("Multiple service loaders present for " + c);
      }
    }

    if (rv == null) { throw new AssertionError("No service loader found for " + c); }

    return rv;

  }

}
