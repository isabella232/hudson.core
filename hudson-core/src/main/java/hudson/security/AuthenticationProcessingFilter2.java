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
 *    Kohsuke Kawaguchi, Matthew R. Harrah
 *
 *
 *******************************************************************************/ 

package hudson.security;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * {@link AuthenticationProcessingFilter} with a change for Hudson so that we
 * can pick up the hidden "from" form field defined in <tt>login.jelly</tt> to
 * send the user back to where he came from, after a successful authentication.
 *
 * @author Kohsuke Kawaguchi
 */
public class AuthenticationProcessingFilter2 extends UsernamePasswordAuthenticationFilter {
    
    private static final Logger LOGGER = Logger.getLogger(AuthenticationProcessingFilter2.class.getName());

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        SimpleUrlAuthenticationFailureHandler handler = (SimpleUrlAuthenticationFailureHandler) super.getFailureHandler();
        handler.setDefaultFailureUrl("/loginError");
        super.unsuccessfulAuthentication(request, response, failed); 
        LOGGER.log(Level.INFO, "Login attempt Failed - ", failed.getLocalizedMessage());
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        SavedRequestAwareAuthenticationSuccessHandler handler = (SavedRequestAwareAuthenticationSuccessHandler) super.getSuccessHandler();
        String targetUrl = request.getParameter("from");
        if ((targetUrl != null) && !targetUrl.equals("")) {
            handler.setDefaultTargetUrl(targetUrl);
        }else{
            handler.setDefaultTargetUrl("/");
        }
        super.successfulAuthentication(request, response, chain, authResult); 
        LOGGER.log(Level.INFO, "Login attempt successful for user", authResult.getPrincipal());
    }
    
    
    

//    @Override
//    protected String determineTargetUrl(HttpServletRequest request) {
//        String targetUrl = request.getParameter("from");
//        request.getSession().setAttribute("from", targetUrl);
//
//        if (targetUrl == null) {
//            return getDefaultTargetUrl();
//        }
//
//        // URL returned from determineTargetUrl() is resolved against the context path,
//        // whereas the "from" URL is resolved against the top of the website, so adjust this.
//        if (targetUrl.startsWith(request.getContextPath())) {
//            return targetUrl.substring(request.getContextPath().length());
//        }
//
//        // not sure when this happens, but apparently this happens in some case.
//        // see #1274
//        return targetUrl;
//    }
//
//    /**
//     * @see
//     * org.springframework.security.ui.AbstractProcessingFilter#determineFailureUrl(javax.servlet.http.HttpServletRequest,
//     * org.springframework.security.AuthenticationException)
//     */
//    @Override
//    protected String determineFailureUrl(HttpServletRequest request, AuthenticationException failed) {
//        Properties excMap = getExceptionMappings();
//        String failedClassName = failed.getClass().getName();
//        String whereFrom = request.getParameter("from");
//        request.getSession().setAttribute("from", whereFrom);
//        return excMap.getProperty(failedClassName, getAuthenticationFailureUrl());
//    }
//
//    /**
//     * Leave the information about login failure.
//     *
//     * <p> Otherwise it seems like Spring Security doesn't really leave the
//     * detail of the failure anywhere.
//     */
//    @Override
//    protected void onUnsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
//        super.onUnsuccessfulAuthentication(request, response, failed);
//        LOGGER.log(Level.INFO, "Login attempt failed", failed);
//    }
//    private static final Logger LOGGER = Logger.getLogger(AuthenticationProcessingFilter2.class.getName());
}
