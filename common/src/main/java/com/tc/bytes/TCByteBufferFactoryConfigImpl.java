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
package com.tc.bytes;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

public class TCByteBufferFactoryConfigImpl implements TCByteBufferFactoryConfig {

  @Override
  public boolean isDisabled() {
    return !(TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.TC_BYTEBUFFER_POOLING_ENABLED));
  }

  @Override
  public int getPoolMaxBufCount() {
    return (TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.TC_BYTEBUFFER_THREADLOCAL_POOL_MAXCOUNT, 2000));
  }

  @Override
  public int getCommonPoolMaxBufCount() {
    return (TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.TC_BYTEBUFFER_COMMON_POOL_MAXCOUNT, 3000));
  }

}
