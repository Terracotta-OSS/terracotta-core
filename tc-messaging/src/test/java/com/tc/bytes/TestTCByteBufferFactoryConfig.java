/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.tc.bytes;

/**
 *
 * @author cdennis
 */
public class TestTCByteBufferFactoryConfig implements TCByteBufferFactoryConfig {

  @Override
  public boolean isDisabled() {
    return false;
  }

  @Override
  public int getPoolMaxBufCount() {
    return 2000;
  }

  @Override
  public int getCommonPoolMaxBufCount() {
    return 3000;
  }
  
}
