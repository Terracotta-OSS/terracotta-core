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

/**
 * A little utility class that will write class files to disk for uninstrumented class files.
 */
public class InitialClassDumper extends AbstractClassDumper {
  
  public final static InitialClassDumper INSTANCE = new InitialClassDumper();
  
  private InitialClassDumper() {
    // make the default constructor private to turn this class into a singleton
  }

  @Override
  protected String getDumpDirectoryName() {
    return "initial";
  }

  @Override
  protected String getPropertyName() {
    return "tc.classloader.writeToDisk.initial";
  }
}
