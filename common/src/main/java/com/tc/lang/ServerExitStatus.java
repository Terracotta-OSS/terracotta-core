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
package com.tc.lang;

public interface ServerExitStatus {
  /**
   * RMP-309 : Error code to convey auto-restart of TC server needed
   */
  public static final short EXITCODE_RESTART_REQUEST = 11;

  /**
   * Error codes during the Server start
   */
  public static final short EXITCODE_STARTUP_ERROR   = 2;

  /**
   * Error code on other fatal condition
   */
  public static final short EXITCODE_FATAL_ERROR     = 3;
}
