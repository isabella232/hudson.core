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

package hudson.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import hudson.model.Hudson;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.RememberMeServices;

/**
 * {@link RememberMeServices} proxy.
 *
 * <p> In Hudson, we need {@link Hudson} instance to perform remember-me
 * service, because it relies on {@link Hudson#getSecretKey()}. However,
 * security filters can be initialized before Hudson is initialized. (See #1210
 * for example.)
 *
 * <p> So to work around the problem, we use a proxy.
 *
 * @author Kohsuke Kawaguchi
 */
public class RememberMeServicesProxy implements RememberMeServices {

    private volatile RememberMeServices delegate;

    public Authentication autoLogin(HttpServletRequest request, HttpServletResponse response) {
        RememberMeServices d = delegate;
        if (d != null) {
            return d.autoLogin(request, response);
        }
        return null;
    }

    public void loginFail(HttpServletRequest request, HttpServletResponse response) {
        RememberMeServices d = delegate;
        if (d != null) {
            d.loginFail(request, response);
        }
    }

    public void loginSuccess(HttpServletRequest request, HttpServletResponse response, Authentication successfulAuthentication) {
        RememberMeServices d = delegate;
        if (d != null) {
            d.loginSuccess(request, response, successfulAuthentication);
        }
    }

    public void setDelegate(RememberMeServices delegate) {
        this.delegate = delegate;
    }
}
