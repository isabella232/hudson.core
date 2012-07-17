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
 *    Kohsuke Kawaguchi, Winston Prakash
 *
 *
 *******************************************************************************/ 
package hudson.security;

import org.springframework.security.ldap.SpringSecurityContextSource;

import java.util.logging.Logger;
import java.util.logging.Level;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.Authentication;
import org.springframework.security.providers.ldap.authenticator.BindAuthenticator;

/**
 * {@link BindAuthenticator} with improved diagnostics.
 *
 */
public class BindAuthenticator2 extends BindAuthenticator {

    /**
     * If we ever had a successful authentication,
     */
    private boolean hadSuccessfulAuthentication;

    public BindAuthenticator2(SpringSecurityContextSource springSecurityContextSource) {
        super(springSecurityContextSource);
    }

    @Override
    public DirContextOperations authenticate(Authentication authentication) {
        DirContextOperations dirContextOperations = super.authenticate(authentication);
        hadSuccessfulAuthentication = true;
        return dirContextOperations;
    }

    @Override
    protected void handleBindException(String userDn, String username, Throwable cause) {
        LOGGER.log(hadSuccessfulAuthentication ? Level.FINE : Level.WARNING,
                "Failed to bind to LDAP: userDn" + userDn + "  username=" + username, cause);
        super.handleBindException(userDn, username, cause);
    }
    private static final Logger LOGGER = Logger.getLogger(BindAuthenticator2.class.getName());
}
