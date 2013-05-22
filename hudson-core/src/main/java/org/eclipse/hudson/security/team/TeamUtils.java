/*
 * Copyright (c) 2013 Oracle Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Winston Prakash
 */

package org.eclipse.hudson.security.team;

import hudson.Functions;
import hudson.model.Hudson;
import hudson.security.SecurityRealm;
import hudson.security.UserMayOrMayNotExistException;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.hudson.security.HudsonSecurityEntitiesHolder;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.security.userdetails.UsernameNotFoundException;

/**
 *
 * @author wjprakash
 */
public class TeamUtils {

    private static final int SID_USER = 1;
    private static final int SID_GROUP = 2;
    private static final int SID_MAY_OR_MAYNOT_EXIST = 3;
    private static final int SID_UNKNOWN = 4;

    public static String getDisplayHtml(String sid) {
        String escSysAdminSid = Functions.escape(sid);

        if (getUserType(sid) == SID_UNKNOWN) {
            escSysAdminSid += " (Unknown)";
        }
        return getIcon(sid) + escSysAdminSid;
    }

    private static int getUserType(String sid) {
        SecurityRealm sr = HudsonSecurityEntitiesHolder.getHudsonSecurityManager().getSecurityRealm();

        // system reserved group
        if (sid.equals("authenticated")) {
            return SID_GROUP;
        }
        try {
            sr.loadUserByUsername(sid);
            return SID_USER;
        } catch (UserMayOrMayNotExistException e) {
            return SID_MAY_OR_MAYNOT_EXIST;
        } catch (UsernameNotFoundException e) {
            // fall through next
        } catch (DataAccessException e) {
            // fall through next
        }

        try {
            sr.loadGroupByGroupname(sid);
            return SID_GROUP;
        } catch (UserMayOrMayNotExistException e) {
            // undecidable, meaning the group may exist
            return SID_MAY_OR_MAYNOT_EXIST;
        } catch (UsernameNotFoundException e) {
            // fall through next
        } catch (DataAccessException e) {
            // fall through next
        }
        return SID_UNKNOWN;
    }

    private static String getIcon(String sid) {
        int sidType = getUserType(sid);
        switch (sidType) {
            case SID_USER:
                return makeImg("person.png");
            case SID_GROUP:
                return makeImg("user.png");
            case SID_MAY_OR_MAYNOT_EXIST:
                return makeImg("warning.png");
            default:
                return makeImg("error.png");
        }
    }

    private static String makeImg(String png) {
        return String.format("<img src='%s%s/images/16x16/%s' style='margin-right:0.2em'>", Stapler.getCurrentRequest().getContextPath(), Hudson.RESOURCE_PATH, png);
    }
    
    public static class ErrorHttpResponse implements HttpResponse {

        private String message;

        ErrorHttpResponse(String message) {
            this.message = message;
        }

        @Override
        public void generateResponse(StaplerRequest sr, StaplerResponse rsp, Object o) throws IOException, ServletException {
            rsp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            rsp.setContentType("text/plain;charset=UTF-8");
            PrintWriter w = new PrintWriter(rsp.getWriter());
            w.println(message);
            w.close();
        }
    }
}
