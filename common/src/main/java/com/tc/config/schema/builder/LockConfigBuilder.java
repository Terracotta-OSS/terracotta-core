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
package com.tc.config.schema.builder;

public interface LockConfigBuilder {

  public static final String TAG_AUTO_LOCK  = "autolock";
  public static final String TAG_NAMED_LOCK = "named-lock";

  public void setLockName(String value);

  public void setMethodExpression(String value);

  public static final String LEVEL_WRITE             = "write";
  public static final String LEVEL_READ              = "read";
  public static final String LEVEL_CONCURRENT        = "concurrent";
  public static final String LEVEL_SYNCHRONOUS_WRITE = "synchronous-write";

  public void setLockLevel(String value);

  public void setLockName(int value);

}
