/*******************************************************************************
 *
 * Copyright (c) 2004-2009 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 
 *    Kohsuke Kawaguchi
 *
 *
 *******************************************************************************/ 

package hudson.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.apache.commons.codec.binary.Base64;

/**
 * Used when storing passwords in configuration files.
 *
 * <p> This doesn't make passwords secure, but it prevents unwanted exposure to
 * passwords, such as when one is grepping the file system or looking at config
 * files for trouble-shooting.
 *
 * @author Kohsuke Kawaguchi
 * @see Protector
 */
public class Scrambler {

    public static String scramble(String secret) {
        if (secret == null) {
            return null;
        }
        try {
            return new String(Base64.encodeBase64(secret.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            throw new Error(e); // impossible
        }
    }

    public static String descramble(String scrambled) {
        if (scrambled == null) {
            return null;
        }
        try {
            return new String(Base64.decodeBase64(scrambled), "UTF-8");
        } catch (IOException e) {
            return "";  // corrupted data.
        }
    }
}
