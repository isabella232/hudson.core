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
 *******************************************************************************/ 

package hudson.security;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import hudson.Extension;
import hudson.Functions;
import hudson.diagnosis.OldDataMonitor;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.View;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import hudson.util.RobustReflectionConverter;
import hudson.util.VersionNumber;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import net.sf.json.JSONObject;
import org.eclipse.hudson.security.HudsonSecurityEntitiesHolder;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Role-based authorization via a matrix.
 *
 * @author Kohsuke Kawaguchi
 */
// TODO: think about the concurrency commitment of this class
public class GlobalMatrixAuthorizationStrategy extends AuthorizationStrategy {

    private transient SidACL acl = new AclImpl();
    /**
     * List up all permissions that are granted.
     *
     * Strings are either the granted authority or the principal, which is not
     * distinguished.
     */
    private final Map<Permission, Set<String>> grantedPermissions = new HashMap<Permission, Set<String>>();
    private final Set<String> sids = new HashSet<String>();

    /**
     * Adds to {@link #grantedPermissions}. Use of this method should be limited
     * during construction, as this object itself is considered immutable once
     * populated.
     */
    public void add(Permission p, String sid) {
        if (p == null) {
            throw new IllegalArgumentException();
        }
        Set<String> set = grantedPermissions.get(p);
        if (set == null) {
            grantedPermissions.put(p, set = new HashSet<String>());
        }
        set.add(sid);
        sids.add(sid);
    }

    /**
     * Works like {@link #add(Permission, String)} but takes both parameters
     * from a single string of the form <tt>PERMISSIONID:sid</tt>
     */
    private void add(String shortForm) {
        int idx = shortForm.indexOf(':');
        Permission p = Permission.fromId(shortForm.substring(0, idx));
        if (p != null) {
            add(p, shortForm.substring(idx + 1));
        } else {
            // This should not happen if Hudson is fully initialized.
            // But Initial Setup also loads Security setup before Hudson Initialization
            if (Hudson.getInstance() != null) {
                throw new IllegalArgumentException("Failed to parse '" + shortForm + "' --- no such permission");
            }
        }
    }

    @Override
    public SidACL getRootACL() {
        return acl;
    }

    public Set<String> getGroups() {
        return sids;
    }

    /**
     * Due to HUDSON-2324, we want to inject Item.READ permission to everyone
     * who has Hudson.READ, to remain backward compatible.
     *
     * @param grantedPermissions
     */
    /*package*/ static boolean migrateHudson2324(Map<Permission, Set<String>> grantedPermissions) {
        boolean result = false;
        // Hudson may not be initialized yet in case of Initial Setup
        if (Hudson.getInstance() == null) {
            return false;
        }
        if (Hudson.getInstance().isUpgradedFromBefore(new VersionNumber("1.300.*"))) {
            Set<String> f = grantedPermissions.get(Hudson.READ);
            if (f != null) {
                Set<String> t = grantedPermissions.get(Item.READ);
                if (t != null) {
                    result = t.addAll(f);
                } else {
                    t = new HashSet<String>(f);
                    result = true;
                }
                grantedPermissions.put(Item.READ, t);
            }
        }
        return result;
    }

    /**
     * Checks if the given SID has the given permission.
     */
    public boolean hasPermission(String sid, Permission p) {
        for (; p != null; p = p.impliedBy) {
            Set<String> set = grantedPermissions.get(p);
            if (set != null && set.contains(sid) && p.getEnabled()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the permission is explicitly given, instead of implied through
     * {@link Permission#impliedBy}.
     */
    public boolean hasExplicitPermission(String sid, Permission p) {
        Set<String> set = grantedPermissions.get(p);
        return set != null && set.contains(sid) && p.getEnabled();
    }

    /**
     * Returns all SIDs configured in this matrix, minus "anonymous"
     *
     * @return Always non-null.
     */
    public List<String> getAllSIDs() {
        Set<String> r = new HashSet<String>();
        for (Set<String> set : grantedPermissions.values()) {
            r.addAll(set);
        }
        r.remove("anonymous");

        String[] data = r.toArray(new String[r.size()]);
        Arrays.sort(data);
        return Arrays.asList(data);
    }

    private final class AclImpl extends SidACL {

        protected Boolean hasPermission(Sid p, Permission permission) {
            if (GlobalMatrixAuthorizationStrategy.this.hasPermission(toString(p), permission)) {
                return true;
            }
            return null;
        }
    }
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    /**
     * Persist {@link GlobalMatrixAuthorizationStrategy} as a list of IDs that
     * represent {@link GlobalMatrixAuthorizationStrategy#grantedPermissions}.
     */
    public static class ConverterImpl implements Converter {

        public boolean canConvert(Class type) {
            return type == GlobalMatrixAuthorizationStrategy.class;
        }

        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            GlobalMatrixAuthorizationStrategy strategy = (GlobalMatrixAuthorizationStrategy) source;

            // Output in alphabetical order for readability.
            SortedMap<Permission, Set<String>> sortedPermissions = new TreeMap<Permission, Set<String>>(Permission.ID_COMPARATOR);
            sortedPermissions.putAll(strategy.grantedPermissions);
            for (Entry<Permission, Set<String>> e : sortedPermissions.entrySet()) {
                String p = e.getKey().getId();
                List<String> sids = new ArrayList<String>(e.getValue());
                Collections.sort(sids);
                for (String sid : sids) {
                    writer.startNode("permission");
                    writer.setValue(p + ':' + sid);
                    writer.endNode();
                }
            }

        }

        public Object unmarshal(HierarchicalStreamReader reader, final UnmarshallingContext context) {
            GlobalMatrixAuthorizationStrategy as = create();

            while (reader.hasMoreChildren()) {
                reader.moveDown();
                try {
                    as.add(reader.getValue());
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(GlobalMatrixAuthorizationStrategy.class.getName())
                            .log(Level.WARNING, "Skipping a non-existent permission", ex);
                    RobustReflectionConverter.addErrorInContext(context, ex);
                }
                reader.moveUp();
            }

            if (migrateHudson2324(as.grantedPermissions)) {
                OldDataMonitor.report(context, "1.301");
            }

            return as;
        }

        protected GlobalMatrixAuthorizationStrategy create() {
            return new GlobalMatrixAuthorizationStrategy();
        }
    }

    public static class DescriptorImpl extends Descriptor<AuthorizationStrategy> {

        protected DescriptorImpl(Class<? extends GlobalMatrixAuthorizationStrategy> clazz) {
            super(clazz);
        }

        public DescriptorImpl() {
        }

        public String getDisplayName() {
            return Messages.GlobalMatrixAuthorizationStrategy_DisplayName();
        }

        @Override
        public AuthorizationStrategy newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            GlobalMatrixAuthorizationStrategy gmas = create();
            for (Map.Entry<String, JSONObject> r : (Set<Map.Entry<String, JSONObject>>) formData.getJSONObject("data").entrySet()) {
                String sid = r.getKey();
                for (Map.Entry<String, Boolean> e : (Set<Map.Entry<String, Boolean>>) r.getValue().entrySet()) {
                    if (e.getValue()) {
                        Permission p = Permission.fromId(e.getKey());
                        gmas.add(p, sid);
                    }
                }
            }
            return gmas;
        }

        protected GlobalMatrixAuthorizationStrategy create() {
            return new GlobalMatrixAuthorizationStrategy();
        }

        public List<PermissionGroup> getAllGroups() {
            List<PermissionGroup> groups = new ArrayList<PermissionGroup>(PermissionGroup.getAll());
            groups.remove(PermissionGroup.get(Permission.class));
            return groups;
        }

        public boolean showPermission(Permission p) {
            // These three are only used by Team Authorization
            if (p == Computer.READ){
                return false;
            }
            if (p == Computer.CREATE){
                return false;
            }
            if (p == View.READ){
                return false;
            }
            return p.getEnabled();
        }

        public FormValidation doCheckName(@QueryParameter String value) throws IOException, ServletException {
            return doCheckName(value, Hudson.getInstance(), Hudson.ADMINISTER);
        }

        FormValidation doCheckName(String value, AccessControlled subject, Permission permission) throws IOException, ServletException {
            if (!subject.hasPermission(permission)) {
                return FormValidation.ok(); // can't check
            }
            final String v = value.substring(1, value.length() - 1);
            SecurityRealm sr = HudsonSecurityEntitiesHolder.getHudsonSecurityManager().getSecurityRealm();
            String ev = Functions.escape(v);

            if (v.equals("authenticated")) // system reserved group
            {
                return FormValidation.respond(Kind.OK, makeImg("user.png") + ev);
            }

            try {
                sr.loadUserByUsername(v);
                return FormValidation.respond(Kind.OK, makeImg("person.png") + ev);
            } catch (UserMayOrMayNotExistException e) {
                // undecidable, meaning the user may exist
                return FormValidation.respond(Kind.OK, ev);
            } catch (UsernameNotFoundException e) {
                // fall through next
            } catch (DataAccessException e) {
                // fall through next
            }

            try {
                sr.loadGroupByGroupname(v);
                return FormValidation.respond(Kind.OK, makeImg("user.png") + ev);
            } catch (UserMayOrMayNotExistException e) {
                // undecidable, meaning the group may exist
                return FormValidation.respond(Kind.OK, ev);
            } catch (UsernameNotFoundException e) {
                // fall through next
            } catch (DataAccessException e) {
                // fall through next
            }

            // couldn't find it. it doesn't exist
            return FormValidation.respond(Kind.ERROR, makeImg("error.png") + ev);
        }

        private String makeImg(String png) {
            return String.format("<img src='%s%s/images/16x16/%s' style='margin-right:0.2em'>", Stapler.getCurrentRequest().getContextPath(), Hudson.RESOURCE_PATH, png);
        }
    }
}
