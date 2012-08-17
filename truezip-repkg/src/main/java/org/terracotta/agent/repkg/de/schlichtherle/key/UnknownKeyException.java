/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * Copyright 2006 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terracotta.agent.repkg.de.schlichtherle.key;

import java.security.GeneralSecurityException;

/**
 * Thrown to indicate that the retrieval of the key to open or create a
 * protected resource has failed.
 * The subclass provides more information.
 *
 * @author Christian Schlichtherle
 * @since TrueZIP 6.0
 * @version @version@
 */
public class UnknownKeyException extends GeneralSecurityException {

    UnknownKeyException() {
    }

    protected UnknownKeyException(String msg) {
        super(msg);
    }
}
