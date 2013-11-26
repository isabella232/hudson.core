/**
 * *****************************************************************************
 *
 * Copyright (c) 2004-2009 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 * Kohsuke Kawaguchi
 *
 *
 ******************************************************************************
 */
package hudson.security;

import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.acls.sid.PrincipalSid;
import org.springframework.security.acls.sid.GrantedAuthoritySid;
import org.springframework.security.acls.sid.Sid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ACL} that checks permissions based on {@link GrantedAuthority} of the
 * {@link Authentication}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class SidACL extends ACL {

    private static final Logger LOGGER = LoggerFactory.getLogger(SidACL.class);

    @Override
    public boolean hasPermission(Authentication a, Permission permission) {
        if (a == SYSTEM) {
            LOGGER.debug("hasPermission(" + a + "," + permission + ")=>SYSTEM user has full access");
            return true;
        }
        Boolean b = _hasPermission(a, permission);

        LOGGER.debug("hasPermission(" + a + "," + permission + ")=>" + (b == null ? "null, thus false" : b));

        if (b == null) {
            b = false;    // default to rejection
        }
        return b;
    }

    /**
     * Implementation that backs up
     * {@link #hasPermission(Authentication, Permission)}.
     *
     * @return true or false if {@link #hasPermission(Sid, Permission)} returns
     * it. Otherwise null, indicating that this ACL doesn't have any entry for
     * it.
     */
    protected Boolean _hasPermission(Authentication a, Permission permission) {
        // ACL entries for this principal takes precedence
        LOGGER.debug("Checking if principal " + a.getName() + " has " + permission);
        Boolean b = hasPermission(new PrincipalSid(a), permission);
        if (b != null) {
            LOGGER.debug("hasPermission(PrincipalSID:" + a.getPrincipal() + "," + permission + ")=>" + b);
            return b;
        }

        // after that, we check if the groups this principal belongs to
        // has any ACL entries.
        // here we are using GrantedAuthority as a group
        for (GrantedAuthority ga : a.getAuthorities()) {
            LOGGER.debug("Checking if principal's role " + ga.getAuthority() + " has " + permission);
            b = hasPermission(new GrantedAuthoritySid(ga), permission);
            if (b != null) {
                LOGGER.debug("hasPermission(GroupSID:" + ga.getAuthority() + "," + permission + ")=>" + b);
                return b;
            }
        }

        // permissions granted to 'everyone' and 'anonymous' users are granted to everyone
        for (Sid sid : AUTOMATIC_SIDS) {
            b = hasPermission(sid, permission);
            if (b != null) {
                LOGGER.debug("hasPermission(" + sid + "," + permission + ")=>" + b);
                return b;
            }
        }

        return null;
    }

    /**
     * Checks if the given {@link Sid} has the given {@link Permission}.
     *
     * <p> {@link #hasPermission(Authentication, Permission)} is implemented by
     * checking authentication's {@link GrantedAuthority} by using this method.
     *
     * <p> It is the implementor's responsibility to recognize
     * {@link Permission#impliedBy} and take that into account.
     *
     * @return true if the access should be granted, false if it should be
     * denied. The null value indicates that the ACL does no rule for this
     * Sid/Permission combination. The caller can decide what to do &mash; such
     * as consulting the higher level ACL, or denying the access (if the model
     * is no-access-by-default.)
     */
    protected abstract Boolean hasPermission(Sid p, Permission permission);

    protected String toString(Sid p) {
        if (p instanceof GrantedAuthoritySid) {
            return ((GrantedAuthoritySid) p).getGrantedAuthority();
        }
        if (p instanceof PrincipalSid) {
            return ((PrincipalSid) p).getPrincipal();
        }
        if (p == EVERYONE) {
            return "role_everyone";
        }
        // hmm...
        return p.toString();
    }

    /**
     * Creates a new {@link SidACL} that first consults 'this' {@link SidACL}
     * and then delegate to the given parent {@link SidACL}. By doing this at
     * the {@link SidACL} level and not at the {@link ACL} level, this allows
     * the child ACLs to have an explicit deny entry. Note that the combined ACL
     * calls hasPermission(Sid,Permission) in the child and parent SidACLs
     * directly, so if these override _hasPermission then this custom behavior
     * will not be applied.
     */
    public final SidACL newInheritingACL(final SidACL parent) {
        final SidACL child = this;
        return new SidACL() {
            protected Boolean hasPermission(Sid p, Permission permission) {
                Boolean b = child.hasPermission(p, permission);
                if (b != null) {
                    return b;
                }
                return parent.hasPermission(p, permission);
            }
        };
    }
}
