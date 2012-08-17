/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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

package org.terracotta.agent.repkg.de.schlichtherle.util.zip;

import java.io.*;
import java.util.*;

/**
 * Drop-in replacement for {@link java.util.zip.ZipEntry java.util.zip.ZipEntry}.
 * For every numeric property of this class, the default value is
 * <code>UNKNOWN</code> in order to indicate an unknown state and it's permitted
 * to set this value explicitly in order to reset the property.
 * <p>
 * Note that a <code>ZipEntry</code> object can be used with only one
 * {@link ZipFile} or {@link ZipOutputStream} instance.
 * Reusing the same <code>ZipEntry</code> object with a second object of these
 * classes is an error and may result in unpredictable behaviour.
 *
 * @author Christian Schlichtherle
 * @version @version@
 */
public class ZipEntry implements Cloneable {

    // Bit indices for initialized fields.
    private static final byte   NAME     = 0, PLATFORM = 1, GENERAL  = 2,
                                METHOD   = 3, DOS_TIME = 4, CRC      = 5,
                                CSIZE    = 6, SIZE     = 7;

    /** The unknown value for numeric properties. */
    public static final byte UNKNOWN = -1;

    /** Windows/DOS/FAT platform. */
    public static final short PLATFORM_FAT  = ZIP.PLATFORM_FAT;

    /** Unix platform. */
    public static final short PLATFORM_UNIX = ZIP.PLATFORM_UNIX;

    /** Compression method for uncompressed (<i>stored</i>) entries. */
    public static final int STORED = ZIP.STORED;

    /** Compression method for compressed (<i>deflated</i>) entries. */
    public static final int DEFLATED = ZIP.DEFLATED;

    /** Smallest supported DOS date/time field value in a ZIP file. */
    public static final long MIN_DOS_TIME = ZIP.MIN_DOS_TIME;

    private byte init;                  // bit flag for init state
    private String name;
    private byte platform = UNKNOWN;    // 1 byte unsigned int
    private short general = UNKNOWN;    // 2 bytes unsigned int
    private short method = UNKNOWN;     // 2 bytes unsigned int
    private int dosTime = UNKNOWN;      // dos time as 4 bytes unsigned int
    private int crc = UNKNOWN;          // 4 bytes unsigned int
    private int csize = UNKNOWN;        // 4 bytes unsigned int
    private int size = UNKNOWN;         // 4 bytes unsigned int
    private byte[] extra;               // null if no extra field
    private String comment;             // null if no comment field

    /** Meaning depends on using class. */
    long offset = UNKNOWN;

    /**
     * Creates a new zip entry with the specified name.
     */
    public ZipEntry(final String name) {
        setName0(name);
    }

    /**
     * Creates a new zip blueprint with fields taken from the specified
     * blueprint.
     */
    public ZipEntry(final ZipEntry blueprint) {
        init = blueprint.init;
        name = blueprint.name;
        platform = blueprint.platform;
        general = blueprint.general;
        method = blueprint.method;
        dosTime = blueprint.dosTime;
        crc = blueprint.crc;
        csize = blueprint.csize;
        size = blueprint.size;
        extra = blueprint.extra;
        comment = blueprint.comment;
        offset = blueprint.offset;
        setInit(NAME, false); // unlock name
    }

    public Object clone() {
        try {
            final ZipEntry entry = (ZipEntry) super.clone();
            entry.setInit(NAME, false); // unlock name
            return entry;
        } catch (CloneNotSupportedException cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
    }

    private final boolean isInit(final byte index) {
        assert 0 <= index && index < 8 : "bit index out of range" + index;
        return (init & (1 << index)) != 0;
    }

    private final void setInit(final byte index, final boolean init) {
        assert 0 <= index && index < 8 : "bit index out of range" + index;
        if (init)
            this.init |=   1 << index;
        else
            this.init &= ~(1 << index);
    }

    /** Returns the ZIP entry name. */
    public String getName() {
        return name;
    }

    final int getNameLength(final String charset)
    throws UnsupportedEncodingException {
        return name != null ? name.getBytes(charset).length : 0;
    }

    /**
     * Resets the ZIP entry name.
     * This method can be called at most once and only if this entry has
     * been created with the {@link #ZipEntry(ZipEntry) copy constructor}
     * or the {@link #clone} method.
     *
     * @since TrueZIP 6.0
     */
    protected void setName(final String name) {
        setName0(name);
    }

    private void setName0(final String name) {
        if (isInit(NAME))
            throw new IllegalStateException("name has already been set");
        if (name == null)
            throw new NullPointerException("name must not be null");
        if (name.length() > 0xffff)
            throw new IllegalArgumentException("name too long");
        setInit(NAME, true);
        this.name = name;
    }

    /**
     * Returns true if and only if this ZIP entry represents a directory entry
     * (i.e. end with <code>'/'</code>).
     */
    public boolean isDirectory() {
        return name.endsWith("/");
    }

    public short getPlatform() {
        return isInit(PLATFORM) ? (short) (platform & 0xFF) : UNKNOWN;
    }

    public void setPlatform(final short platform) {
        if (platform < UNKNOWN || 0xff < platform)
            throw new IllegalArgumentException(
                    name + ": invalid platform: " + platform);
        setInit(PLATFORM, platform != UNKNOWN);
        this.platform = (byte) platform;
    }

    int getGeneral() {
        return isInit(GENERAL) ? general & 0xffff : UNKNOWN;
    }

    void setGeneral(final int general) {
        if (general < UNKNOWN || 0xffff < general)
            throw new IllegalArgumentException(name
            + ": invalid general purpose bit flag: " + general);
        setInit(GENERAL, general != UNKNOWN);
        this.general = (short) general;
    }

    final boolean getGeneralBit(int index) {
        if (!isInit(GENERAL))
            throw new IllegalStateException(name
            + ": general purpose bit flag not initialized");
        if (index < 0 || 15 < index)
            throw new IllegalArgumentException(name
            + ": general purpose bit index out of range: " + index);
        return (general & (1 << index)) != 0;
    }

    final void setGeneralBit(int index, boolean bit) {
        if (index < 0 || 15 < index)
            throw new IllegalArgumentException(name
            + ": general purpose bit index out of range: " + index);
        setInit(GENERAL, true);
        if (bit)
            general |=   1 << index;
        else
            general &= ~(1 << index);
    }

    public int getMethod() {
        return isInit(METHOD) ? method & 0xffff : UNKNOWN;
    }

    public void setMethod(final int method) {
        if (method < UNKNOWN || 0xffff < method)
            throw new IllegalArgumentException(
                    name + ": invalid compression method: " + method);
        setInit(METHOD, method != UNKNOWN);
        this.method = (short) method;
    }

    protected long getDosTime() {
        return isInit(DOS_TIME) ? dosTime & 0xffffffffL : UNKNOWN;
    }

    protected void setDosTime(final long dosTime) {
        if (dosTime < UNKNOWN || 0xffffffffL < dosTime)
            throw new IllegalArgumentException(
                    name + ": invalid DOS date/time field value: " + dosTime);
        setInit(DOS_TIME, dosTime != UNKNOWN);
        this.dosTime = (int) dosTime;
    }

    public long getTime() {
        return isInit(DOS_TIME) ? dos2javaTime(dosTime & 0xffffffffL) : UNKNOWN;
    }

    public void setTime(final long time) {
        setDosTime(time != UNKNOWN ? java2dosTime(time) : UNKNOWN);
    }

    public long getCrc() {
        return isInit(CRC) ? crc & 0xffffffffL : UNKNOWN;
    }

    public void setCrc(final long crc) {
        if (crc < UNKNOWN || 0xffffffffL < crc)
            throw new IllegalArgumentException(
                    name + ": invalid CRC-32: " + crc);
        setInit(CRC, crc != UNKNOWN);
        this.crc = (int) crc;
    }

    public long getCompressedSize() {
        return isInit(CSIZE) ? csize & 0xffffffffL : UNKNOWN;
    }

    public void setCompressedSize(final long csize) {
        if (csize < - 1 || 0xffffffffL < csize)
            throw new IllegalArgumentException(
                    name + ": invalid compressed size: " + csize);
        setInit(CSIZE, csize != UNKNOWN);
        this.csize = (int) csize;
    }

    public long getSize() {
        return isInit(SIZE) ? size & 0xffffffffL : UNKNOWN;
    }

    public void setSize(final long size) {
        if (size < UNKNOWN || 0xffffffffL < size)
            throw new IllegalArgumentException(
                    name + ": invalid size: " + size);
        setInit(SIZE, size != UNKNOWN);
        this.size = (int) size;
    }

    public byte[] getExtra() {
        return extra != null ? (byte[]) extra.clone() : null;
    }

    final int getExtraLength() {
        return extra != null ? extra.length : 0;
    }

    public void setExtra(final byte[] extra) {
        if (extra != null && 0xffff < extra.length)
            throw new IllegalArgumentException(
                    name + ": extra field length too long: " + extra.length);
        this.extra = extra != null ? (byte[]) extra.clone() : null;
    }

    public String getComment() {
        return comment;
    }

    final int getCommentLength(String charset)
    throws UnsupportedEncodingException {
        return comment != null ? comment.getBytes(charset).length : 0;
    }

    public void setComment(final String comment) {
        if (comment != null && 0xffff < comment.length())
            throw new IllegalArgumentException(
                    name + ": comment length too long: " + comment.length());
        this.comment = comment;
    }

    /** Returns the ZIP entry name. */
    public String toString() {
        return getName();
    }

    //
    // Time conversion.
    //

    /**
     * Converts a DOS date/time field value to Java time value.
     *
     * @param dosTime The DOS date/time field value.
     * @return The number of milliseconds from the epoch.
     * @throws IllegalArgumentException If <code>time</code> is negative.
     */
    protected static long dos2javaTime(final long dosTime) {
        if (dosTime < 0 || 0xffffffffL < dosTime)
            throw new IllegalArgumentException(
                    "invalid DOS date/time field value: " + dosTime);

        final int time = (int) dosTime;
        final Calendar cal = (Calendar) calendar.get();
        cal.set(Calendar.YEAR, ((time >> 25) & 0xff) + 1980);
        cal.set(Calendar.MONTH, ((time >> 21) & 0x0f) - 1);
        cal.set(Calendar.DATE, (time >> 16) & 0x1f);
        cal.set(Calendar.HOUR_OF_DAY, (time >> 11) & 0x1f);
        cal.set(Calendar.MINUTE, (time >> 5) & 0x3f);
        cal.set(Calendar.SECOND, (time << 1) & 0x3e);
        // According to the ZIP file format specification, its internal time
        // has only two seconds granularity.
        // Make calendar return only total seconds in order to make this work
        // correctly.
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Converts a Java time value to a DOS date/time field value.
     * The result is rounded to even seconds and is minimum
     * <code>MIN_DOS_TIME</code>.
     *
     * @param time The number of milliseconds from the epoch.
     * @return The DOS date/time field value for the given time.
     * @throws IllegalArgumentException If <code>time</code> is negative.
     *         or would result in a dos date/time field value before
     *         <code>MIN_DOS_TIME</code>.
     */
    protected static long java2dosTime(final long time) {
        if (time < 0)
            throw new IllegalArgumentException(
                    "invalid modification time: " + time);

        final Calendar cal = (Calendar) calendar.get();
        cal.setTimeInMillis(time);
        int year = cal.get(Calendar.YEAR);
        if (year < 1980)
            return MIN_DOS_TIME;
        final long dosTime = (((year - 1980) & 0xff) << 25)
        | ((cal.get(Calendar.MONTH) + 1) << 21)
        | (cal.get(Calendar.DAY_OF_MONTH) << 16)
        | (cal.get(Calendar.HOUR_OF_DAY) << 11)
        | (cal.get(Calendar.MINUTE) << 5)
        | (cal.get(Calendar.SECOND) >> 1);
        assert dosTime >= MIN_DOS_TIME;
        return dosTime;
    }

    private static final ThreadLocal calendar = new ThreadLocal() {
        protected Object initialValue() {
            return new GregorianCalendar();
        }
    };
}