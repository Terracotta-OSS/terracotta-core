/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.util;

import sun.reflect.ReflectionFactory;

import com.tc.exception.TCRuntimeException;

import java.lang.reflect.Constructor;

/**
 * A wrapper for unsafe usage in class like Atomic Variables, ReentrantLock, etc.
 */
@SuppressWarnings("restriction")
public class ReflectionUtil {
  private static final Constructor       refConstructor;
  private static final ReflectionFactory rf = ReflectionFactory.getReflectionFactory();

  static {
    try {
      refConstructor = Object.class.getDeclaredConstructor(new Class[0]);
    } catch (NoSuchMethodException e) {
      throw new TCRuntimeException(e);
    }
  }

  private ReflectionUtil() {
    // Disallow any object to be instantiated.
  }

  public static Constructor newConstructor(Class clazz) {
    return rf.newConstructorForSerialization(clazz, refConstructor);
  }

}
