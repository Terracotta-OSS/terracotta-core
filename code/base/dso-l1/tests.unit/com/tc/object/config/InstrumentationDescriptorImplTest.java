/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.config;

import com.tc.object.config.schema.InstrumentedClass;
import com.tc.object.config.schema.TestInstrumentedClass;

import junit.framework.TestCase;

public class InstrumentationDescriptorImplTest extends TestCase {
  

public void testBasics() {
    InstrumentedClass ic = new TestInstrumentedClass();
    TestClassExpressionMatcher em = new TestClassExpressionMatcher();
    InstrumentationDescriptor idi = new InstrumentationDescriptorImpl(ic, em);
    
    String expression = "classExpression";
    em.shouldMatch = false;
    assertFalse(idi.matches(expression));
    em.shouldMatch = true;
    assertTrue(idi.matches(expression));

  }

}
