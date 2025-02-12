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