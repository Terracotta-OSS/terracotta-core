/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * ChannelReadOnlyFile.java
 *
 * Created on 17. Dezember 2005, 18:36
 */
/*
 * Copyright 2005-2007 Schlichtherle IT Services
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

package org.terracotta.agent.repkg.de.schlichtherle.io.rof;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * A {@link ReadOnlyFile} implementation using file channels.
 *
 * @author Christian Schlichtherle
 * @version @version@
 */
public class ChannelReadOnlyFile extends AbstractReadOnlyFile {

    /** For use by {@link #read()} only! */
    private final ByteBuffer singleByteBuffer = ByteBuffer.allocate(1);

    private final FileChannel channel;

    public ChannelReadOnlyFile(File file) throws FileNotFoundException {
        channel = new FileInputStream(file).getChannel();
    }

    public long length() throws IOException {
        return channel.size();
    }

    public long getFilePointer() throws IOException {
        return channel.position();
    }

    public void seek(long fp) throws IOException {
        try {
            channel.position(fp);
        } catch (IllegalArgumentException iae) {
            final IOException ioe = new IOException(iae.toString());
            ioe.initCause(iae);
            throw ioe;
        }
    }

    public int read() throws IOException {
        singleByteBuffer.position(0);
        if (channel.read(singleByteBuffer) == 1)
            return singleByteBuffer.array()[0] & 0xff;
        else
            return -1;
    }

    public int read(byte[] buf, int off, int len) throws IOException {
        return channel.read(ByteBuffer.wrap(buf, off, len));
    }

    public void close() throws IOException {
        channel.close();
    }
}
