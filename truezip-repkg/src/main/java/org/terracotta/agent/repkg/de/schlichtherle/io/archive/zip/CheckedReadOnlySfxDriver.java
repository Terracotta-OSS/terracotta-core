/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * CheckedReadOnlySfxDriver.java
 *
 * Created on 30. Juni 2006, 00:06
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


import java.io.*;
import java.util.zip.*;

import javax.swing.*;

import org.terracotta.agent.repkg.de.schlichtherle.io.archive.*;
import org.terracotta.agent.repkg.de.schlichtherle.io.rof.*;

/**
 * An archive driver for SFX/EXE files which checks the CRC-32 value for all
 * ZIP entries in input archives.
 * The additional CRC-32 computation makes this class slower than its super
 * class.
 * <p>
 * If there is a mismatch of the CRC-32 values for a ZIP entry in an input
 * archive, the {@link InputStream#close} method of the corresponding stream
 * for the archive entry will throw a
 * {@link org.terracotta.agent.repkg.de.schlichtherle.util.zip.CRC32Exception}.
 * This exception is then propagated through the stack up to the corresponding
 * file operation in the package <code>de.schlichtherle.io</code> where it is
 * either allowed to pass on or is catched and processed accordingly.
 * For example, the {@link org.terracotta.agent.repkg.de.schlichtherle.io.FileInputStream#close()}
 * method would allow the <code>CRC32Exception</code> to pass on to the
 * client application, whereas the
 * {@link org.terracotta.agent.repkg.de.schlichtherle.io.File#catTo(OutputStream)} method would simply
 * return <code>false</code>.
 * Other than this, the archive entry will be processed normally.
 * So if just the CRC-32 value for the entry in the archive file has been
 * modified, you can still read its entire contents.
 * <p>
 * Instances of this class are immutable.
 * 
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.1
 * @see ReadWriteSfxDriver
 * @see CheckedZip32InputArchive
 */
public class CheckedReadOnlySfxDriver extends ReadOnlySfxDriver {
    private static final long serialVersionUID = -940108057195872802L;

    /**
     * Equivalent to {@link #CheckedReadOnlySfxDriver(String, Icon, Icon, boolean, int)
     * this(DEFAULT_CHARSET, null, null, false, DEFAULT_LEVEL)}.
     */
    public CheckedReadOnlySfxDriver() {
        this(DEFAULT_CHARSET, null, null, false, DEFAULT_LEVEL);
    }

    /**
     * Equivalent to {@link #CheckedReadOnlySfxDriver(String, Icon, Icon, boolean, int)
     * this(charset, null, null, false, DEFAULT_LEVEL)}.
     */
    public CheckedReadOnlySfxDriver(String charset) {
        this(charset, null, null, false, DEFAULT_LEVEL);
    }

    /**
     * Equivalent to {@link #CheckedReadOnlySfxDriver(String, Icon, Icon, boolean, int)
     * this(DEFAULT_CHARSET, null, null, false, level)}.
     */
    public CheckedReadOnlySfxDriver(int level) {
        this(DEFAULT_CHARSET, null, null, false, level);
    }

    /** Constructs a new checked read-only SFX/EXE driver. */
    public CheckedReadOnlySfxDriver(
            String charset,
            Icon openIcon,
            Icon closedIcon,
            boolean postambled,
            final int level) {
        super(charset, openIcon, closedIcon, postambled, level);
    }
    
    protected Zip32InputArchive createZip32InputArchive(
            Archive archive,
            ReadOnlyFile rof)
    throws IOException {
        return new CheckedZip32InputArchive(
                rof, getCharset(), getPreambled(), getPostambled());
    }
}
