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
package com.tc.test;

/**
 * generates a sequence of unique ints for all kinds of test purposes.
 * NOTE: this sequence starts from 0 and will be reset every time JVM is restarted.
 */
public class UniqueSequenceGenerator {

  public static UniqueSequenceGenerator getInstance() {
    return theInstance;
  }
  
  public synchronized int getNextInt() {
    return this.currentValue++;
  }
  
  private UniqueSequenceGenerator() {
    super();
  }

  private static UniqueSequenceGenerator theInstance = new UniqueSequenceGenerator();
  
  private int currentValue = 84925;
}
