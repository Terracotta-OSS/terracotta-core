/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
