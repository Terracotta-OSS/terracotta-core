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
package com.tc.l2.logging;

import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class StartupAndSizeBasedTriggeringPolicy<E> extends SizeBasedTriggeringPolicy<E> {
    private final AtomicBoolean firstTime = new AtomicBoolean();

    public boolean isTriggeringEvent(final File activeFile, final E event) {
        if (firstTime.compareAndSet(false, true) && activeFile.length() > 0) {
            return true;
        }
        return super.isTriggeringEvent(activeFile, event);
    }
}