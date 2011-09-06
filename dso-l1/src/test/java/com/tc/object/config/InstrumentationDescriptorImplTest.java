/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.object.bytecode.aspectwerkz.ClassInfoFactory;
import com.tc.object.config.schema.InstrumentedClass;
import com.tc.object.config.schema.TestInstrumentedClass;

import junit.framework.TestCase;

public class InstrumentationDescriptorImplTest extends TestCase {
  

public void testBasics() {
    InstrumentedClass ic = new TestInstrumentedClass();
    TestClassExpressionMatcher em = new TestClassExpressionMatcher();
    InstrumentationDescriptor idi = new InstrumentationDescriptorImpl(ic, em);
    
    String expression = "classExpression";
    ClassInfoFactory classInfoFactory = new ClassInfoFactory();
    ClassInfo classInfo = classInfoFactory.getClassInfo(expression);
    
    em.shouldMatch = false;
    assertFalse(idi.matches(classInfo));
    em.shouldMatch = true;
    assertTrue(idi.matches(classInfo));
  }

}
