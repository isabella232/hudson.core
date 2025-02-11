/*******************************************************************************
 *
 * Copyright (c) 2004-2012 Oracle Corporation.
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

import hudson.Functions;
import hudson.model.Hudson;
import hudson.util.Scrambler;
import java.io.IOException;
import java.net.URLEncoder;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.hudson.security.HudsonSecurityEntitiesHolder;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Implements the dual authentcation mechanism.
 *
 * <p> Hudson supports both the HTTP basic authentication and the form-based
 * authentication. The former is for scripted clients, and the latter is for
 * humans. Unfortunately, becase the servlet spec does not allow us to
 * programatically authenticate users, we need to rely on some work around to
 * make it work, and this is the class that implements that work around.
 *
 * <p> When an HTTP request arrives with an HTTP basic auth header, this filter
 * detects that and emulate an invocation of <tt>/j_security_check</tt> (see <a
 * href="http://mail-archives.apache.org/mod_mbox/tomcat-users/200105.mbox/%3C9005C0C9C85BD31181B20060085DAC8B10C8EF@tuvi.andmevara.ee%3E">this
 * page</a> for the original technique.)
 *
 * <p> This causes the container to perform authentication, but there's no way
 * to find out whether the user has been successfully authenticated or not. So
 * to find this out, we then redirect the user to
 * {@link Hudson#doSecured(org.kohsuke.stapler.StaplerRequest, org.kohsuke.stapler.StaplerResponse) <tt>/secured/...</tt> page}.
 *
 * <p> The handler of the above URL checks if the user is authenticated, and if
 * not report an HTTP error code. Otherwise the user is redirected back to the
 * original URL, where the request is served.
 *
 * <p> So all in all, the redirection works like <tt>/abc/def</tt> ->
 * <tt>/secured/abc/def</tt> -> <tt>/abc/def</tt>.
 *
 * <h2>Notes</h2> <ul> <li> The technique of getting a request dispatcher for
 * <tt>/j_security_check</tt> may not work for all containers, but so far that
 * seems like the only way to make this work. <li> This A->B->A redirect is a
 * cyclic redirection, so we need to watch out for clients that detect this as
 * an error. </ul>
 *
 * @author Kohsuke Kawaguchi
 */
public class BasicAuthenticationFilter implements Filter {

    private ServletContext servletContext;

    public void init(FilterConfig filterConfig) throws ServletException {
        servletContext = filterConfig.getServletContext();
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse rsp = (HttpServletResponse) response;
        String authorization = req.getHeader("Authorization");

        String path = req.getServletPath();
        if (authorization == null || req.getUserPrincipal() != null || path.startsWith("/secured/")
                || !HudsonSecurityEntitiesHolder.getHudsonSecurityManager().isUseSecurity()) {
            // normal requests, or security not enabled
            if (req.getUserPrincipal() != null) {
                // before we route this request, integrate the container authentication
                // to Spring Security. For anonymous users that doesn't have user principal,
                // AnonymousProcessingFilter that follows this should create
                // an Authentication object.
                SecurityContextHolder.getContext().setAuthentication(new ContainerAuthentication(req));
            }
            try {
                chain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
            return;
        }

        // authenticate the user
        String username = null;
        String password = null;
        String uidpassword = Scrambler.descramble(authorization.substring(6));
        int idx = uidpassword.indexOf(':');
        if (idx >= 0) {
            username = uidpassword.substring(0, idx);
            password = uidpassword.substring(idx + 1);
        }

        if (username == null) {
            rsp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            rsp.setHeader("WWW-Authenticate", "Basic realm=\"Hudson administrator\"");
            return;
        }

        path = Functions.getHttpRequestRootPath(req) + "/secured" + path;
        String q = req.getQueryString();
        if (q != null) {
            path += '?' + q;
        }

        // prepare a redirect
        rsp.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        rsp.setHeader("Location", path);

        // ... but first let the container authenticate this request
        RequestDispatcher d = servletContext.getRequestDispatcher("/j_security_check?j_username="
                + URLEncoder.encode(username, "UTF-8") + "&j_password=" + URLEncoder.encode(password, "UTF-8"));
        d.include(req, rsp);
    }

    //public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    //    HttpServletRequest req = (HttpServletRequest) request;
    //    String authorization = req.getHeader("Authorization");
    //
    //    String path = req.getServletPath();
    //    if(authorization==null || req.getUserPrincipal()!=null || path.startsWith("/secured/")) {
    //        chain.doFilter(request,response);
    //    } else {
    //        if(req.getQueryString()!=null)
    //            path += req.getQueryString();
    //        ((HttpServletResponse)response).sendRedirect(Functions.getRootPath(req)+"/secured"+path);
    //    }
    //}
    public void destroy() {
    }
}
