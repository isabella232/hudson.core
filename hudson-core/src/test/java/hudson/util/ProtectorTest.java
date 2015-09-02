/*******************************************************************************
 *
 * Copyright (c) 2004-2010 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: 
 *
 *    Roy Varghese
 *     
 *
 *******************************************************************************/ 

package hudson.util;

import junit.framework.TestCase;

public class ProtectorTest extends TestCase {

    public void testEncrypt() {
        // test some encryption
        assertFalse("abc".equals(Protector.protect("abc")));
    }

    public void testDecrypt() {
        assertTrue("abc".equals(Protector.unprotect(Protector.protect(("abc")))));
    }

    
}
