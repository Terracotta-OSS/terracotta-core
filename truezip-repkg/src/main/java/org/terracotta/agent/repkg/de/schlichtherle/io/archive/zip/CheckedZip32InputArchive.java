/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * CheckedZip32InputArchive.java
 *
 * Created on 29. Juni 2006, 20:58
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

package org.terracotta.agent.repkg.de.schlichtherle.io.archive.zip;


import java.io.*;
import java.util.zip.*;

import org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi.*;
import org.terracotta.agent.repkg.de.schlichtherle.io.rof.*;

/**
 * A {@link Zip32InputArchive} which checks the CRC-32 value for all ZIP entries.
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
 * 
 * @see Zip32InputArchive
 * @see CheckedZip32OutputArchive
 * @see CheckedZip32Driver
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.1
 */
public class CheckedZip32InputArchive extends Zip32InputArchive {
    
    public CheckedZip32InputArchive(
            ReadOnlyFile rof,
            String charset,
            boolean preambled,
            boolean postambled)
    throws  NullPointerException,
            UnsupportedEncodingException,
            FileNotFoundException,
            ZipException,
            IOException {
        super(rof, charset, preambled, postambled);
    }

    /**
     * Overridden to read from a checked input stream.
     */
    public InputStream getInputStream(
            ArchiveEntry entry,
            ArchiveEntry dstEntry)
    throws  IOException {
        return super.getInputStream(
                entry.getName(), true, !(dstEntry instanceof Zip32Entry));
    }
}