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
package com.tc.handler;

import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import com.tc.text.DumpLoggerWriter;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinterImpl;

import java.io.PrintWriter;

public class CallbackDumpAdapter implements CallbackOnExitHandler {

  private final PrettyPrintable dumpObject;

  public CallbackDumpAdapter(PrettyPrintable dumpObject) {
    this.dumpObject = dumpObject;
  }

  @Override
  public void callbackOnExit(CallbackOnExitState state) {
    DumpLoggerWriter writer = new DumpLoggerWriter();
    writer.write("\n***********************************************************************************\n");
    PrintWriter pw = new PrintWriter(writer);
    PrettyPrinterImpl prettyPrinter = new PrettyPrinterImpl(pw);
    prettyPrinter.autoflush(false);
    prettyPrinter.visit(dumpObject);
    writer.flush();
  }
}
