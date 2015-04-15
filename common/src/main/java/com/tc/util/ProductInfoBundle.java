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

import java.util.ListResourceBundle;

public class ProductInfoBundle extends ListResourceBundle {
  @Override
  public Object[][] getContents() {
    return contents;
  }

  static final Object[][] contents = {
    {"moniker", "Terracotta"},
    {"invalid.timestamp", "The build timestamp string ''{0}'' does not appear to be valid."},
    {"load.properties.failure", "Unable to load build properties from ''{0}''."},
    {"copyright", "Copyright (c) 2003-2008 Terracotta, Inc. All rights reserved."},
    {"option.verbose", "Produces more detailed information."},
    {"option.raw", "Produces raw information."},
    {"option.help", "Shows this text."}
  };
}
