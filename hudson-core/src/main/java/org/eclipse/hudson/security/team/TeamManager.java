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

import com.thoughtworks.xstream.XStream;
import hudson.BulkChange;
import hudson.XmlFile;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import hudson.security.ACL;
import hudson.security.Permission;
import hudson.security.SecurityRealm;
import hudson.util.FormValidation;
import hudson.util.XStream2;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.eclipse.hudson.security.HudsonSecurityEntitiesHolder;
import org.eclipse.hudson.security.HudsonSecurityManager;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manger that manages the teams and their persistence
 *
 * @since 3.1.0
 * @author Winston Prakash
 */
public final class TeamManager implements Saveable {

    private List<String> sysAdmins = new CopyOnWriteArrayList<String>();
    private List<Team> teams = new CopyOnWriteArrayList<Team>();
    private transient final XStream xstream = new XStream2();
    private transient Logger logger = LoggerFactory.getLogger(TeamManager.class);
    private transient File hudsonHomeDir;
    private transient File teamsFolder;
    private transient final String teamsConfigFileName = "teams.xml";
    private transient DefaultTeam defaultTeam;
    private transient final String TEAMS_FOLDER_NAME = "teams";

    public TeamManager(File homeDir) {
        hudsonHomeDir = homeDir;
        teamsFolder = new File(hudsonHomeDir, TEAMS_FOLDER_NAME);
        if (!teamsFolder.exists()) {
            teamsFolder.mkdirs();
        }
        initializeXstream();
        load();
        ensureDefaultTeam();
    }

    public void addSysAdmin(String adminName) throws IOException {
        if (!sysAdmins.contains(adminName)) {
            sysAdmins.add(adminName);
            save();
        }
    }

    public void removeSysAdmin(String adminName) throws IOException {
        if (sysAdmins.contains(adminName)) {
            sysAdmins.remove(adminName);
            save();
        }
    }

    public List<String> getSysAdmins() {
        return sysAdmins;
    }

    public boolean isSysAdmin(String userName) {
        boolean isSysAdmin;
        HudsonSecurityManager hudsonSecurityManager = HudsonSecurityEntitiesHolder.getHudsonSecurityManager();
        SecurityRealm securityRealm = null;
        if (hudsonSecurityManager != null) {
            securityRealm = hudsonSecurityManager.getSecurityRealm();
        }
        if ((securityRealm != null) && securityRealm instanceof TeamAwareSecurityRealm) {
            TeamAwareSecurityRealm teamAwareSecurityRealm = (TeamAwareSecurityRealm) securityRealm;
            isSysAdmin = teamAwareSecurityRealm.isCurrentUserSysAdmin();
        } else {
            isSysAdmin = sysAdmins.contains(userName);
        }
        return isSysAdmin;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public Team createTeam(String teamName, String description) throws IOException, TeamAlreadyExistsException {
        for (Team team : teams) {
            if (teamName.equals(team.getName())) {
                throw new TeamAlreadyExistsException(teamName);
            }
        }

        Team newTeam = new Team(teamName, description, this);
        teams.add(newTeam);
        save();
        return newTeam;
    }

    public Team createTeam(String teamName) throws IOException, TeamAlreadyExistsException {
        return createTeam(teamName, teamName);
    }

    public void deleteTeam(String teamName) throws TeamNotFoundException, IOException {
        Team team = findTeam(teamName);
        teams.remove(team);
        save();
    }

    public HttpResponse doCreateTeam(@QueryParameter String teamName, @QueryParameter String description) throws IOException {
        if (!Hudson.getInstance().getSecurityManager().hasPermission(Permission.HUDSON_ADMINISTER)) {
            return HttpResponses.forbidden();
        }
        if ((teamName == null) || "".equals(teamName.trim())) {
            return new TeamUtils.ErrorHttpResponse("Team name required");
        }
        try {
            Team team = createTeam(teamName, description);
            return HttpResponses.forwardToView(team, "team.jelly");
        } catch (TeamAlreadyExistsException ex) {
            return new TeamUtils.ErrorHttpResponse(ex.getLocalizedMessage());
        }
    }

    public HttpResponse doDeleteTeam(@QueryParameter String teamName) throws IOException {
        if (!Hudson.getInstance().getSecurityManager().hasPermission(Permission.HUDSON_ADMINISTER)) {
            return HttpResponses.forbidden();
        }
        if ((teamName == null) || "".equals(teamName.trim())) {
            return new TeamUtils.ErrorHttpResponse("Team name required");
        }
        try {
            deleteTeam(teamName);
            return HttpResponses.ok();
        } catch (TeamNotFoundException ex) {
            return new TeamUtils.ErrorHttpResponse(ex.getLocalizedMessage());
        }
    }

    public HttpResponse doAddTeamAdmin(@QueryParameter String teamName, @QueryParameter String teamAdminSid) throws IOException {
        if (!Hudson.getInstance().getSecurityManager().hasPermission(Permission.HUDSON_ADMINISTER)) {
            return HttpResponses.forbidden();
        }
        if ((teamName == null) || "".equals(teamName.trim())) {
            return new TeamUtils.ErrorHttpResponse("Team name required");
        }
        if ((teamAdminSid == null) || "".equals(teamAdminSid.trim())) {
            return new TeamUtils.ErrorHttpResponse("Team admin name required");
        }
        Team team;
        try {
            team = findTeam(teamName);
        } catch (TeamNotFoundException ex) {
            return new TeamUtils.ErrorHttpResponse(teamName + " is not a valid team.");
        }

        if (!team.getAdmins().contains(teamAdminSid)) {
            team.addAdmin(teamAdminSid);
            return FormValidation.respond(FormValidation.Kind.OK, TeamUtils.getIcon(teamAdminSid));
        } else {
            return new TeamUtils.ErrorHttpResponse(teamAdminSid + " is already a team admin.");
        }
    }

    public HttpResponse doRemoveTeamAdmin(@QueryParameter String teamName, @QueryParameter String teamAdminSid) throws IOException {
        if (!Hudson.getInstance().getSecurityManager().hasPermission(Permission.HUDSON_ADMINISTER)) {
            return HttpResponses.forbidden();
        }
        if ((teamName == null) || "".equals(teamName.trim())) {
            return new TeamUtils.ErrorHttpResponse("Team name required");
        }
        if ((teamAdminSid == null) || "".equals(teamAdminSid.trim())) {
            return new TeamUtils.ErrorHttpResponse("Team admin name required");
        }
        Team team;
        try {
            team = findTeam(teamName);
        } catch (TeamNotFoundException ex) {
            return new TeamUtils.ErrorHttpResponse(teamName + " is not a valid team.");
        }

        if (team.getAdmins().contains(teamAdminSid)) {
            team.removeAdmin(teamAdminSid);
            return HttpResponses.ok();
        } else {
            return new TeamUtils.ErrorHttpResponse(teamAdminSid + " is not a team admin.");
        }
    }

    public HttpResponse doAddTeamMember(@QueryParameter String teamName, @QueryParameter String teamMemberSid) throws IOException {
        if (!Hudson.getInstance().getSecurityManager().hasPermission(Permission.HUDSON_ADMINISTER)) {
            return HttpResponses.forbidden();
        }
        if ((teamName == null) || "".equals(teamName.trim())) {
            return new TeamUtils.ErrorHttpResponse("Team name required");
        }
        if ((teamMemberSid == null) || "".equals(teamMemberSid.trim())) {
            return new TeamUtils.ErrorHttpResponse("Team member name required");
        }
        Team team;
        try {
            team = findTeam(teamName);
        } catch (TeamNotFoundException ex) {
            return new TeamUtils.ErrorHttpResponse(teamName + " is not a valid team.");
        }

        if (!team.getMembers().contains(teamMemberSid)) {
            team.addMember(teamMemberSid);
            return FormValidation.respond(FormValidation.Kind.OK, TeamUtils.getIcon(teamMemberSid));
        } else {
            return new TeamUtils.ErrorHttpResponse(teamMemberSid + " is already a team member.");
        }
    }

    public HttpResponse doRemoveTeamMember(@QueryParameter String teamName, @QueryParameter String teamMemberSid) throws IOException {
        if (!Hudson.getInstance().getSecurityManager().hasPermission(Permission.HUDSON_ADMINISTER)) {
            return HttpResponses.forbidden();
        }
        if ((teamName == null) || "".equals(teamName.trim())) {
            return new TeamUtils.ErrorHttpResponse("Team name required");
        }
        if ((teamMemberSid == null) || "".equals(teamMemberSid.trim())) {
            return new TeamUtils.ErrorHttpResponse("Team member name required");
        }
        Team team;
        try {
            team = findTeam(teamName);
        } catch (TeamNotFoundException ex) {
            return new TeamUtils.ErrorHttpResponse(teamName + " is not a valid team.");
        }

        if (team.getMembers().contains(teamMemberSid)) {
            team.removeMember(teamMemberSid);
            return HttpResponses.ok();
        } else {
            return new TeamUtils.ErrorHttpResponse(teamMemberSid + " is not a team member.");
        }
    }

    public HttpResponse doCheckSid(@QueryParameter String sid) throws IOException {
        return FormValidation.respond(FormValidation.Kind.OK, TeamUtils.getIcon(sid));
    }

    public void addUser(String teamName, String userName) throws TeamNotFoundException, IOException {
        Team team = findTeam(teamName);
        team.addMember(userName);
        save();
    }

    public Team findTeam(String teamName) throws TeamNotFoundException {
        for (Team team : teams) {
            if (teamName.equals(team.getName())) {
                return team;
            }
        }
        throw new TeamNotFoundException(teamName);
    }

    public void removeTeam(String teamName) throws IOException, TeamNotFoundException {
        Team team = findTeam(teamName);
        teams.remove(team);
        save();
    }

    public Team findCurrentUserTeam() {
        Team team;
        HudsonSecurityManager hudsonSecurityManager = HudsonSecurityEntitiesHolder.getHudsonSecurityManager();
        SecurityRealm securityRealm = null;
        if (hudsonSecurityManager != null) {
            securityRealm = hudsonSecurityManager.getSecurityRealm();
        }
        if ((securityRealm != null) && securityRealm instanceof TeamAwareSecurityRealm) {
            TeamAwareSecurityRealm teamAwareSecurityRealm = (TeamAwareSecurityRealm) securityRealm;
            team = teamAwareSecurityRealm.GetCurrentUserTeam();
        } else {
            String currentUser = HudsonSecurityManager.getAuthentication().getName();
            team = findUserTeam(currentUser);
        }
        return team;
    }

    public boolean isCurrentUserHasAccess(String jobName) {
        Team userTeam = findCurrentUserTeam();
        if (userTeam != null) {
            if (userTeam.isJobOwner(jobName)) {
                return true;
            } else {
                return isAnonymousJob(jobName);
            }
        } else {
            return isAnonymousJob(jobName);
        }
    }

    public boolean isUserHasAccess(String userName, String jobName) {
        Team userTeam = findUserTeam(userName);
        if (userTeam != null) {
            return userTeam.isJobOwner(jobName);
        } else {
            for (Team team : teams) {
                if (team.isJobOwner(jobName)) {
                    // Job belongs to a team so has no access
                    return false;
                }
            }
            // Job does not belong to any team, so has access
            return true;
        }
    }

    public Team findUserTeam(String userName) {

        for (Team team : teams) {
            if (team.isMember(userName)) {
                return team;
            }
        }
        HudsonSecurityManager hudsonSecurityManager = HudsonSecurityEntitiesHolder.getHudsonSecurityManager();
        SecurityRealm securityRealm = null;
        if (hudsonSecurityManager != null) {
            securityRealm = hudsonSecurityManager.getSecurityRealm();
        }
        if ((securityRealm != null) && securityRealm instanceof TeamAwareSecurityRealm) {
            TeamAwareSecurityRealm teamAwareSecurityRealm = (TeamAwareSecurityRealm) securityRealm;
            return teamAwareSecurityRealm.GetCurrentUserTeam();
        }
        return null;
    }

    public Team findJobOwnerTeam(String jobName) {
        for (Team team : teams) {
            if (team.isJobOwner(jobName)) {
                return team;
            }
        }
        return null;
    }

    public void addJobToCurrentUserTeam(String jobName) throws IOException {
        addJob(findCurrentUserTeam(), jobName);
    }

    public void addJobToUserTeam(String userName, String jobName) throws IOException {
        addJob(findUserTeam(userName), jobName);

    }

    public void addJob(Team team, String jobName) throws IOException {
        if (team != null) {
            team.addJob(jobName);
            save();
        }
    }

    public void removeJobFromCurrentUserTeam(String jobName) throws IOException {
        removeJob(findCurrentUserTeam(), jobName);
    }

    public void removeJobFromUserTeam(String userName, String jobName) throws IOException {
        removeJob(findUserTeam(userName), jobName);
    }

    public void removeJob(Team team, String jobName) throws IOException {
        if (team != null) {
            team.removeJob(jobName);
            save();
        }
    }

    public void renameJobInCurrentUserTeam(String oldJobName, String newJobName) throws IOException {
        renameJob(findCurrentUserTeam(), oldJobName, newJobName);
    }

    public void renameJobInUserTeam(String userName, String oldJobName, String newJobName) throws IOException {
        renameJob(findUserTeam(userName), oldJobName, newJobName);
    }

    public void renameJob(Team team, String oldJobName, String newJobName) throws IOException {
        if (team != null) {
            team.renameJob(oldJobName, newJobName);
            save();
        }
    }

    /**
     * Save the settings to the configuration file.
     */
    @Override
    public synchronized void save() throws IOException {
        if (useBulkSaveFlag && BulkChange.contains(this)) {
            return;
        }
        getConfigFile().write(this);
        if (useBulkSaveFlag) {
            SaveableListener.fireOnChange(this, getConfigFile());
        }
    }

    public String getTeamQualifiedJobId(String jobId) {
        if (defaultTeam.isJobOwner(jobId)) {
            return jobId;
        }

        Team team = findCurrentUserTeam();
        if ((team != null) && !Team.DEFAULT_TEAM_NAME.equals(team.getName())) {
            jobId = team.getName() + "_" + jobId;
        }
        return jobId;
    }

    public String getJobsFolderName(String jobId) {
        Team team = findJobOwnerTeam(jobId);
        // May be just created job
        if (team == null) {
            team = findCurrentUserTeam();
        }
        if ((team != null) && !Team.DEFAULT_TEAM_NAME.equals(team.getName())) {
            return TEAMS_FOLDER_NAME + "/" + team.getName() + "/" + Team.JOBS_FOLDER_NAME;
        }
        return "jobs";
    }

    public File[] getJobsRootFolders() {
        List<File> jobsRootFolders = new ArrayList<File>();
        for (Team team : teams) {
            if (Team.DEFAULT_TEAM_NAME.equals(team.getName())) {
                jobsRootFolders.addAll(team.getJobsRootFolders(hudsonHomeDir));
            } else {
                jobsRootFolders.addAll(team.getJobsRootFolders(teamsFolder));
            }
        }
        return jobsRootFolders.toArray(new File[jobsRootFolders.size()]);
    }

    public static class TeamNotFoundException extends Exception {

        public TeamNotFoundException(String teamName) {
            super("Team " + teamName + " does not exist.");
        }
    }

    public static class TeamAlreadyExistsException extends Exception {

        public TeamAlreadyExistsException(String teamName) {
            super("Team " + teamName + " already exists.");
        }
    }

    void setUseBulkSaveFlag(boolean flag) {
        useBulkSaveFlag = flag;
    }

    /**
     * Get an ACL that provides system wide authorization
     *
     * @return TeamBasedACL with SYSTEM scope
     */
    ACL getRoolACL() {
        return new TeamBasedACL(this, TeamBasedACL.SCOPE.GLOBAL);
    }

    /**
     * Get an ACL that provides job specific authorization
     *
     * @return TeamBasedACL with JOB scope
     */
    ACL getACL(Job<?, ?> job) {
        return new TeamBasedACL(this, TeamBasedACL.SCOPE.JOB, job);
    }

    /**
     * Get an ACL that provides team specific authorization
     *
     * @return TeamBasedACL with JOB scope
     */
    ACL getACL(Team team) {
        return new TeamBasedACL(this, TeamBasedACL.SCOPE.JOB, team);
    }

    Team getDefaultTeam() throws TeamNotFoundException {
        return findTeam(DefaultTeam.DEFAULT_TEAM_NAME);
    }

    private boolean isAnonymousJob(String jobName) {
        for (Team team : teams) {
            if (team.isJobOwner(jobName)) {
                // job belongs to another team so has no access
                return false;
            }
        }
        // Not belong to any team, so has access
        return true;
    }

    /**
     * The file where the teams settings are saved.
     */
    private XmlFile getConfigFile() {
        return new XmlFile(xstream, new File(teamsFolder, teamsConfigFileName));
    }
    // This is purely fo unit test. Since Hudson is not fully loaded during
    // test BulkChange saving mode is not available
    private transient boolean useBulkSaveFlag = true;

    /**
     * Load the settings from the configuration file
     */
    private void load() {
        XmlFile config = getConfigFile();
        try {
            if (config.exists()) {
                config.unmarshal(this);
            }
        } catch (IOException e) {
            logger.error("Failed to load " + config, e);
        }
    }

    private void ensureDefaultTeam() {
        defaultTeam = new DefaultTeam(this);
        try {
            Team team = findTeam(DefaultTeam.DEFAULT_TEAM_NAME);
            teams.remove(team);
        } catch (TeamNotFoundException ex) {
            // It's ok, we are going to remove it any way
        }
        try {
            defaultTeam.loadExistingJobs(hudsonHomeDir);
        } catch (IOException ex) {
            logger.error("Failed to load existing jobs", ex);
        }
        teams.add(defaultTeam);
    }
    
    private void initializeXstream(){
        xstream.alias("teamManager", TeamManager.class);
        xstream.alias("team", Team.class);
    }
}
