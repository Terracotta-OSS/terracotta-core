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
