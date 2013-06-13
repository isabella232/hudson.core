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

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import hudson.model.Item;
import hudson.security.Permission;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * A simple model to hold team members information
 *
 * @since 3.1.0
 * @author Winston Prakash
 */
public class TeamMember {

    private String name;
    private boolean isTeamAdmin;
    private Set<Permission> grantedPermissions = new HashSet<Permission>();
    private Set<Permission> teamAdminGrantedPermissions = new HashSet<Permission>();
    
    public TeamMember(){
        setTeamAdminGrantedPermissions();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isTeamAdmin() {
        return isTeamAdmin;
    }

    void setAsTeamAdmin(boolean teamAdmin) {
        this.isTeamAdmin = teamAdmin;
    }

    void addPermission(Permission permission) {
        if (!grantedPermissions.contains(permission)) {
            grantedPermissions.add(permission);
        }
    }

    void removePermission(Permission permission) {
        if (grantedPermissions.contains(permission)) {
            grantedPermissions.remove(permission);
        }
    }

    public boolean hasPermission(Permission permission) {
        if (isTeamAdmin) {
            return teamAdminGrantedPermissions.contains(permission);
        }
        return grantedPermissions.contains(permission);
    }

    public boolean canConfigure() {
        if (isTeamAdmin) {
            return true;
        }
        return hasPermission(Item.CONFIGURE);
    }

    public boolean canCreate() {
        if (isTeamAdmin) {
            return true;
        }
        return hasPermission(Item.CREATE);
    }

    public boolean canDelete() {
        if (isTeamAdmin) {
            return true;
        }
        return hasPermission(Item.DELETE);
    }

    public boolean canBuild() {
        if (isTeamAdmin) {
            return true;
        }
        return hasPermission(Item.BUILD);
    }

    void addPermission(String permission) {
        if ("admin".equals(permission)) {
            isTeamAdmin = true;
        }
        if ("create".equals(permission)) {
            grantedPermissions.add(Item.CREATE);
            grantedPermissions.add(Item.EXTENDED_READ);
        }
        if ("delete".equals(permission)) {
            grantedPermissions.add(Item.DELETE);
            grantedPermissions.add(Item.WIPEOUT);
        }
        if ("configure".equals(permission)) {
            grantedPermissions.add(Item.CONFIGURE);
        }
        if ("build".equals(permission)) {
            grantedPermissions.add(Item.BUILD);
        }
        grantedPermissions.add(Item.READ);
        grantedPermissions.add(Item.WORKSPACE);
    }

    private void setTeamAdminGrantedPermissions() {
        teamAdminGrantedPermissions.add(Item.READ);
        teamAdminGrantedPermissions.add(Item.EXTENDED_READ);
        teamAdminGrantedPermissions.add(Item.CREATE);
        teamAdminGrantedPermissions.add(Item.DELETE);
        teamAdminGrantedPermissions.add(Item.WIPEOUT);
        teamAdminGrantedPermissions.add(Item.CONFIGURE);
        teamAdminGrantedPermissions.add(Item.BUILD);
        teamAdminGrantedPermissions.add(Item.WORKSPACE);
    }

    public static class ConverterImpl implements Converter {

        @Override
        public boolean canConvert(Class type) {
            return type == TeamMember.class;
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            TeamMember teamMember = (TeamMember) source;
            writer.startNode("name");
            writer.setValue(teamMember.name);
            writer.endNode();
            StringWriter strWriter = new StringWriter();

            if (teamMember.isTeamAdmin) {
                strWriter.append("admin");
            }
            if (teamMember.canCreate()) {
                strWriter.append(",");
                strWriter.append("create");
            }
            if (teamMember.canDelete()) {
                strWriter.append(",");
                strWriter.append("delete");
            }
            if (teamMember.canBuild()) {
                strWriter.append(",");
                strWriter.append("build");
            }
            if (teamMember.canConfigure()) {
                strWriter.append(",");
                strWriter.append("configure");
            }
            writer.startNode("permissions");
            writer.setValue(strWriter.toString());
            writer.endNode();
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            TeamMember member = new TeamMember();
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                if ("name".equals(reader.getNodeName())) {
                    member.name = reader.getValue();
                }
                if ("permissions".equals(reader.getNodeName())) {
                    String permissions = reader.getValue();
                    for (String permission : permissions.split(",")) {
                        member.addPermission(permission);
                    }
                }
                reader.moveUp();
            }
            return member;
        }
    }
}
