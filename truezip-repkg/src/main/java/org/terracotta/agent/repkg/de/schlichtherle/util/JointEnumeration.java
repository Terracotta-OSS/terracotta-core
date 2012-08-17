/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * Copyright 2007 Schlichtherle IT Services
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

package org.terracotta.agent.repkg.de.schlichtherle.util;

import java.util.*;

/**
 * Concatenates two enumerations.
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.5
 */
public final class JointEnumeration implements Enumeration {
    private Enumeration e1;
    private final Enumeration e2;

    public JointEnumeration(final Enumeration e1, final Enumeration e2) {
        this.e1 = e1;
        this.e2 = e2;
    }

    public boolean hasMoreElements() {
        return e1.hasMoreElements()
           || (e1 != e2 && (e1 = e2).hasMoreElements());
    }

    public Object nextElement() {
        try {
            return e1.nextElement();
        } catch (NoSuchElementException ex) {
            if (e1 == e2)
                throw ex;
            return (e1 = e2).nextElement();
        }
    }
}
