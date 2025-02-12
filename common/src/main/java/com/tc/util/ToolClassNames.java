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

/**
 * This interface defines the class names of various Terracotta tools.
 * The tools themselves are not included in the -api packages but the 
 * class names are so that tc-maven-plugin can invoke the tools 
 * by launching java executions with the names.
 */
public interface ToolClassNames {

  public static final String TC_SERVER_CLASS_NAME = "com.tc.server.TCServerMain";
  public static final String TC_STOP_CLASS_NAME = "com.tc.admin.TCStop";
  
}
