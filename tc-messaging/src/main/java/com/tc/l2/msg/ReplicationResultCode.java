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
package com.tc.l2.msg;

/**
 *
 */
public enum ReplicationResultCode {
  NONE, SUCCESS {
    @Override
    byte code() {
      return 1;
    }
  }, FAIL {
    @Override
    byte code() {
      return 2;
    }
  };

  static ReplicationResultCode decode(int code) {
    switch (code) {
      case 0:
        return NONE;
      case 1:
        return SUCCESS;
      case 2:
        return FAIL;
      default:
        throw new RuntimeException("bad code");
    }
  }

  byte code() {
    return 0;
  }
  
}
