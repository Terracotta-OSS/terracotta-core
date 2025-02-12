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
package com.tc.text;

import org.slf4j.Logger;

import com.tc.logging.TCLogging;

import java.io.IOException;
import java.io.StringWriter;

public class DumpLoggerWriter extends StringWriter {
  private static final Logger logger = TCLogging.getDumpLogger();
  
  @Override
  public void flush() {
    StringBuffer buffer = getBuffer();
    if (buffer.length() <= 0) { return; }
    logger.info(buffer.toString());
    buffer.delete(0, buffer.length());
  }

  @Override
  public void close() throws IOException {
    super.close();
    flush();
  }
}
