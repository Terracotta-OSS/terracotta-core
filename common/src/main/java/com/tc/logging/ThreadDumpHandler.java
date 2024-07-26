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
package com.tc.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.util.runtime.ThreadDumpUtil;

public class ThreadDumpHandler implements CallbackOnExitHandler {

  private static final Logger logger = LoggerFactory.getLogger(ThreadDumpHandler.class);

  public ThreadDumpHandler() {
  }

  @Override
  public void callbackOnExit(CallbackOnExitState state) {
    logger.error(ThreadDumpUtil.getThreadDump());
  }
}
