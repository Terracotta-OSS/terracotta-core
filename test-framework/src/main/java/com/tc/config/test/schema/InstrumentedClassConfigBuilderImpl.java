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
package com.tc.config.test.schema;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;

/**
 * Allows you to build valid config for an instrumented class. This class <strong>MUST NOT</strong> invoke the actual
 * XML beans to do its work; one of its purposes is, in fact, to test that those beans are set up correctly.
 */
public class InstrumentedClassConfigBuilderImpl extends BaseConfigBuilder implements InstrumentedClassConfigBuilder {

  private boolean isInclude;

  public InstrumentedClassConfigBuilderImpl(Class clazz) {
    this();
    setClassExpression(clazz.getName());
  }

  public InstrumentedClassConfigBuilderImpl() {
    //super(4, ALL_PROPERTIES);
    super(5, ALL_PROPERTIES);
    this.isInclude = true;
    
    setArrayPropertyTagName("on-load", "method");
  }

  @Override
  public void setIsInclude(boolean isInclude) {
    this.isInclude = isInclude;
  }

  @Override
  public void setClassExpression(String value) {
    if (isInclude) setProperty("class-expression", value);
    else setProperty("exclude", value);
  }

  @Override
  public void setHonorTransient(String value) {
    setProperty("honor-transient", value);
  }

  @Override
  public void setHonorTransient(boolean value) {
    setProperty("honor-transient", value);
  }

  @Override
  public void setCallConstructorOnLoad(String value) {
    setProperty("call-constructor-on-load", value);
  }

  @Override
  public void setCallConstructorOnLoad(boolean value) {
    setProperty("call-constructor-on-load", value);
  }
  
  @Override
  public void setCallMethodOnLoad(String value) {
    setProperty("on-load", new Object[]{value});
  }

  private static final String[] ALL_PROPERTIES = new String[] { "class-expression", "honor-transient",
      "call-constructor-on-load", "exclude", "on-load"              };

  @Override
  public String toString() {
    String out = "";

    if (this.isInclude) {
      out += indent() + openElement("include");
      out += indent() + elements(ALL_PROPERTIES);
      out += indent() + closeElement("include");
    } else {
      out += indent() + elements(ALL_PROPERTIES);
    }

    return out;
  }

}
