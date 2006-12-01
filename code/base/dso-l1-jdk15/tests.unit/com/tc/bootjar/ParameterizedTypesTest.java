/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bootjar;

import com.tc.object.tools.BootJar;
import com.tc.test.TCTestCase;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Set;

public class ParameterizedTypesTest extends TCTestCase {
  
  public void testParameterizedTypesTest() throws Exception {
    BootJar bj = BootJar.getDefaultBootJarForReading();
    Set specs = bj.getAllPreInstrumentedClasses();
    for (Iterator iter = specs.iterator(); iter.hasNext();) {
      String className = (String) iter.next();
      checkParmeterizedType(className);
    }
    
  }

  private void checkParmeterizedType(String className) throws Exception {
    Class klass = Class.forName(className);
    Type gsc = klass.getGenericSuperclass();
    System.err.println("GenericSuperClass for " + className + " is " + gsc);
  }

}
