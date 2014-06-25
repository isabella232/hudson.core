/**
 * *****************************************************************************
 *
 * Copyright (c) 2010, CloudBees, Inc.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *
 *
 ******************************************************************************
 */

/*
 * The MIT License
 *
 * Copyright (c) 2010, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.util.io;

import hudson.util.IOException2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * {@link OutputStream} that writes to a file.
 *
 * <p>
 * Unlike regular {@link FileOutputStream}, this implementation allows the
 * caller to close, and then keep writing.
 *
 * @author Kohsuke Kawaguchi
 */
public class ReopenableFileOutputStream extends OutputStream {

    private final File out;
    private OutputStream current;
    private boolean appendOnNextOpen = false;
    private int rotateCount;

    public ReopenableFileOutputStream(File out) {
        this(out, 0);
    }

    public ReopenableFileOutputStream(File out, int rotateCount) {
        this.out = out;
        this.rotateCount = rotateCount;
    }

    private synchronized OutputStream current() throws IOException {
        if (current == null) {
            try {
                current = new FileOutputStream(out, appendOnNextOpen);
            } catch (FileNotFoundException e) {
                throw new IOException2("Failed to open " + out, e);
            }
        }
        return current;
    }

    @Override
    public void write(int b) throws IOException {
        byte newlineCharacter = (byte) 0x0A;
        byte byteWritten = (byte) b;
        current().write(b);
        if (byteWritten == newlineCharacter) {
            writeTimestamp();
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        current().write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        byte newlineCharacter = (byte) 0x0A;
        byte byteWritten = (byte) b[0];
        if ((len > 1) && (byteWritten != newlineCharacter)) {
            writeTimestamp();
        }
        current().write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        current().flush();
    }

    private void writeTimestamp() throws IOException {
        Date dNow = new Date();
        SimpleDateFormat ft
                = new SimpleDateFormat("'['yyyy-MM-dd hh:mm:ss']' ' '");
        current().write(ft.format(dNow).getBytes());
    }

    @Override
    public synchronized void close() throws IOException {
        if (current != null) {
            current.close();
            appendOnNextOpen = true;
            current = null;
        }
    }

    /**
     * In addition to close, ensure that the next "open" would truncate the
     * file.
     *
     * @throws java.io.IOException
     */
    public synchronized void rewind() throws IOException {
        close();
        appendOnNextOpen = false;
        for (int i = rotateCount - 1; i >= 0; i--) {
            File rotateFile = getRotateFileName(i);
            if (rotateFile.exists()) {
                File nextRotateFile = getRotateFileName(i + 1);
                nextRotateFile.delete();
                rotateFile.renameTo(nextRotateFile);
            }
        }
    }

    private File getRotateFileName(int rotatedNumber) {
        if (rotatedNumber == 0) {
            return out;
        }
        return new File(out.getPath() + "." + rotatedNumber);
    }
}
