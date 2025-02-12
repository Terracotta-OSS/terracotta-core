/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.tc.util;

import com.tc.exception.TCException;

/**
 * An exception thrown when an operation times out. Feel free to subclass
 * 
 * @author teck
 */
public class TCTimeoutException extends TCException {

  public TCTimeoutException(long timeout) {
    this("Timeout of " + timeout + " occurred");
  }

  public TCTimeoutException(String string) {
    super(string);
  }

  public TCTimeoutException(Throwable cause) {
    super(cause);
  }
  
  public TCTimeoutException(String reason, Throwable cause) {
    super(reason, cause);
  }
}
