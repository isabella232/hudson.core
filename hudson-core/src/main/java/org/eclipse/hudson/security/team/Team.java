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

import hudson.model.Hudson;
import hudson.model.Items;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.security.AuthorizationStrategy;
import hudson.security.Permission;
import hudson.security.SecurityRealm;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.eclipse.hudson.security.HudsonSecurityEntitiesHolder;
import org.eclipse.hudson.security.HudsonSecurityManager;
import org.springframework.security.AccessDeniedException;
import org.springframework.security.Authentication;

/**
 * A simple model to hold team members and name of jobs belong to the team
 *
 * @since 3.1.0
 * @author Winston Prakash
 */
public class Team implements AccessControlled {

    public static final String PUBLIC_TEAM_NAME = "public";
    private List<TeamMember> teamMembers = new CopyOnWriteArrayList<TeamMember>();
    private List<String> jobs = new CopyOnWriteArrayList<String>();
    private String name;
    protected static final String JOBS_FOLDER_NAME = "jobs";
    private String description;
    private transient TeamManager teamManager;

    Team(String name, TeamManager teamManager) {
        this(name, name, teamManager);
    }

    Team(String teamName, String description, TeamManager teamManager) {
        this.name = teamName;
        this.description = description;
        this.teamManager = teamManager;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCurrentUserSysAdmin() {
        String currentUser = HudsonSecurityManager.getAuthentication().getName();
        return isAdmin(currentUser);
    }

    public boolean isAdmin(String userName) {
        // Team Manager ACL always assume userName current user
        boolean isAdmin = false;
        HudsonSecurityManager hudsonSecurityManager = HudsonSecurityEntitiesHolder.getHudsonSecurityManager();
        SecurityRealm securityRealm = null;
        if (hudsonSecurityManager != null) {
            securityRealm = hudsonSecurityManager.getSecurityRealm();
        }
        if ((securityRealm != null) && securityRealm instanceof TeamAwareSecurityRealm) {
            TeamAwareSecurityRealm teamAwareSecurityRealm = (TeamAwareSecurityRealm) securityRealm;
            isAdmin = teamAwareSecurityRealm.isCurrentUserTeamAdmin();
        } else {
            TeamMember member = findTeamMember(userName);
            if (member != null) {
                isAdmin = member.isTeamAdmin();
            }
        }
        return isAdmin;
    }

    public List<TeamMember> getTeamMembers() {
        return Collections.unmodifiableList(teamMembers);
    }

    public TeamMember findTeamMember(String userName) {
        for (TeamMember member : teamMembers) {
            if (userName.equals(member.getName())) {
                return member;
            }
        }
        return null;
    }

    void addMember(String teamMemberSid, boolean isTeamAdmin, boolean canCreate,
            boolean canDelete, boolean canConfigure) throws IOException {
        TeamMember newMember = new TeamMember();
        newMember.setName(teamMemberSid);
        newMember.setAsTeamAdmin(isTeamAdmin);
        if (canCreate) {
            newMember.addPermission(Permission.CREATE);
        }
        if (canDelete) {
            newMember.addPermission(Permission.DELETE);
        }
        if (canConfigure) {
            newMember.addPermission(Permission.CONFIGURE);
        }
        addMember(newMember);
    }

    void updateMember(String teamMemberSid, boolean isTeamAdmin, boolean canCreate,
            boolean canDelete, boolean canConfigure) throws IOException {
        TeamMember currentMember = findTeamMember(teamMemberSid);
        if (currentMember != null) {
            currentMember.setAsTeamAdmin(isTeamAdmin);
            if (canCreate) {
                currentMember.addPermission(Permission.CREATE);
            } else {
                currentMember.removePermission(Permission.CREATE);
            }
            if (canDelete) {
                currentMember.addPermission(Permission.DELETE);
            } else {
                currentMember.removePermission(Permission.DELETE);
            }
            if (canConfigure) {
                currentMember.addPermission(Permission.CONFIGURE);
            } else {
                currentMember.removePermission(Permission.CONFIGURE);
            }
            getTeamManager().save();
        }
    }

    public void addMember(TeamMember member) throws IOException {
        if (!teamMembers.contains(member)) {
            teamMembers.add(member);
            getTeamManager().save();
        }
    }

    public void removeMember(String userName) throws IOException {
        TeamMember member = findTeamMember(userName);
        if (member != null) {
            teamMembers.remove(member);
            getTeamManager().save();
        }
    }

    public boolean isMember(String userName) {
        HudsonSecurityManager hudsonSecurityManager = HudsonSecurityEntitiesHolder.getHudsonSecurityManager();
        SecurityRealm securityRealm = null;
        if (hudsonSecurityManager != null) {
            securityRealm = hudsonSecurityManager.getSecurityRealm();
        }
        if ((securityRealm != null) && securityRealm instanceof TeamAwareSecurityRealm) {
            TeamAwareSecurityRealm teamAwareSecurityRealm = (TeamAwareSecurityRealm) securityRealm;
            Team currentUserTeam = teamAwareSecurityRealm.GetCurrentUserTeam();
            if (currentUserTeam == this) {
                return true;
            } else {
                return false;
            }
        } else {
            return findTeamMember(userName) != null;
        }
    }

    public void addJob(String jobId) throws IOException {
        if (!jobs.contains(jobId)) {
            jobs.add(jobId);
            getTeamManager().save();
        }
    }

    public void removeJob(String jobId) throws IOException {
        if (jobs.contains(jobId)) {
            jobs.remove(jobId);
            getTeamManager().save();
        }
    }

    public boolean isJobOwner(String jobName) {
        return jobs.contains(jobName);
    }

    void renameJob(String oldJobName, String newJobName) throws IOException {
        if (jobs.contains(oldJobName)) {
            jobs.remove(oldJobName);
            jobs.add(newJobName);
            getTeamManager().save();
        }
    }

    List<File> getJobsRootFolders(File rootFolder) {
        File jobsFolder = getJobsFolder(rootFolder);
        if (jobsFolder.exists()) {
            File[] jobsRootFolders = jobsFolder.listFiles(new FileFilter() {
                @Override
                public boolean accept(File child) {
                    return child.isDirectory() && Items.getConfigFile(child).exists();
                }
            });
            if (jobsRootFolders != null) {
                return Arrays.asList(jobsRootFolders);
            }
        }
        return Collections.EMPTY_LIST;
    }

    protected File getJobsFolder(File rootFolder) {
        return new File(rootFolder, name + "/" + JOBS_FOLDER_NAME);
    }

    @Override
    public ACL getACL() {
        AuthorizationStrategy authorizationStrategy = HudsonSecurityEntitiesHolder.getHudsonSecurityManager().getAuthorizationStrategy();
        if (authorizationStrategy instanceof TeamBasedAuthorizationStrategy) {
            TeamBasedAuthorizationStrategy teamBasedAuthorizationStrategy = (TeamBasedAuthorizationStrategy) authorizationStrategy;
            return teamBasedAuthorizationStrategy.getACL(this);
        }
        // Team will not be used if Team Based Authorization Strategy is not used
        return new ACL() {
            public boolean hasPermission(Authentication a, Permission permission) {
                return false;
            }
        };
    }

    @Override
    public void checkPermission(Permission permission) throws AccessDeniedException {
        getACL().checkPermission(permission);
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return getACL().hasPermission(permission);
    }

    // When the Team is unmarshalled it would not have Team Manager set
    private TeamManager getTeamManager() {
        if (teamManager == null) {
            return Hudson.getInstance().getTeamManager();
        } else {
            return teamManager;
        }
    }
}
