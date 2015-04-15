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
package com.tc.object.config;

/**
 * Describe the Custom adaption of a class
 */
public class TransparencyClassSpecImpl implements TransparencyClassSpec {

  private final String               className;
  private final String               changeApplicatorClassName;
  private final ChangeApplicatorSpec changeApplicatorSpec;
  private boolean                    useNonDefaultConstructor = false;

  public TransparencyClassSpecImpl(final String className, final String changeApplicatorClassName) {
    this.className = className;
    this.changeApplicatorClassName = changeApplicatorClassName;
    this.changeApplicatorSpec = new DSOChangeApplicatorSpec(changeApplicatorClassName);
  }

  @Override
  public String getClassName() {
    return className;
  }

  @Override
  public ChangeApplicatorSpec getChangeApplicatorSpec() {
    return changeApplicatorSpec;
  }

  @Override
  public boolean isUseNonDefaultConstructor() {
    return this.useNonDefaultConstructor;
  }

  @Override
  public void setUseNonDefaultConstructor(final boolean useNonDefaultConstructor) {
    this.useNonDefaultConstructor = useNonDefaultConstructor;
  }

  @Override
  public String getChangeApplicatorClassName() {
    return this.changeApplicatorClassName;
  }

}
