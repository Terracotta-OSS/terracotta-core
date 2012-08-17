/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * AbstractSfxDriver.java
 *
 * Created on 24. Dezember 2005, 00:01
 */
/*
 * Copyright 2006-2007 Schlichtherle IT Services
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

package org.terracotta.agent.repkg.de.schlichtherle.io.archive.zip;

import java.util.logging.*;

import javax.swing.*;

/**
 * An archive driver which builds Self Executable (SFX/EXE) ZIP files.
 * <p>
 * Instances of this class are immutable.
 * 
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0
 */
abstract public class AbstractSfxDriver extends Zip32Driver {
    private static final long serialVersionUID = -656184651651654635L;

    private static final String CLASS_NAME
            = "de/schlichtherle/io/archive/zip/AbstractSfxDriver".replace('/', '.'); // beware of code obfuscation!

    /**
     * The character set used in SFX archives by default, which is determined
     * by calling {@code System.getProperty("file.encoding")}.
     */
    public static final String DEFAULT_CHARSET
            = System.getProperty("file.encoding");

    static {
        Logger.getLogger(CLASS_NAME, CLASS_NAME).log(Level.CONFIG, "charset", DEFAULT_CHARSET);
    }

    /**
     * Constructs a new abstract SFX/EXE driver which allows for a preamble.
     * Self Extracting Archives typically use the preamble to store the
     * application code that is required to extract the ZIP file contents.
     */
    protected AbstractSfxDriver(
            String charset,
            Icon openIcon,
            Icon closedIcon,
            boolean postambled,
            final int level) {
        super(charset, openIcon, closedIcon, true, postambled, level);
    }
}
