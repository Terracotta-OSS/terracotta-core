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
package com.tc.net.core;

import com.tc.util.TCServiceLoader;

/**
 *
 */
public class DefaultBufferManagerFactory {
  private static BufferManagerFactory DEFAULT;
  
  public static void setBufferManagerFactory(BufferManagerFactory d) {
    DEFAULT = d;
  }
  
  public static BufferManagerFactory getBufferManagerFactory() {
    if (DEFAULT == null) {
      DEFAULT = (BufferManagerFactory)TCServiceLoader.loadServices(BufferManagerFactory.class).stream().findFirst().orElseThrow(()->new RuntimeException("not found"));
    }
    return DEFAULT;
  }
}
