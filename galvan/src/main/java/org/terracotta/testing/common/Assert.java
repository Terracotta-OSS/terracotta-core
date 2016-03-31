/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.testing.common;


public class Assert {
  public static void unexpected(Throwable t) {
    throw new AssertionError("Unexpected exception", t);
  }

  public static void assertTrue(boolean test) {
    if (!test) {
      throw new AssertionError("Case expected to be true");
    }
  }

  public static void assertFalse(boolean test) {
    if (test) {
      throw new AssertionError("Case expected to be false");
    }
  }

  public static void assertNull(Object expectedNull) {
    if (null != expectedNull) {
      throw new AssertionError("Object expected to be null");
    }
  }

  public static void assertNotNull(Object notNull) {
    if (null == notNull) {
      throw new AssertionError("Object expected to be not null");
    }
  }
}
