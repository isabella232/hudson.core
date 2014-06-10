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
import hudson.model.Computer;
import hudson.model.Item;
import hudson.model.Items;
import hudson.model.View;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.security.AuthorizationStrategy;
import hudson.security.Permission;
import hudson.security.SecurityRealm;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import org.eclipse.hudson.security.HudsonSecurityEntitiesHolder;
import org.eclipse.hudson.security.HudsonSecurityManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

/**
 * A simple model to hold team members and name of jobs belong to the team
 *
 * @since 3.1.0
 * @author Winston Prakash
 */
public class Team implements AccessControlled {

    public static final String PUBLIC_TEAM_NAME = "public";
    private final List<TeamMember> teamMembers = new CopyOnWriteArrayList<TeamMember>();

    private final List<TeamJob> jobs = new CopyOnWriteArrayList<TeamJob>();

    private final List<TeamNode> nodes = new CopyOnWriteArrayList<TeamNode>();

    private final List<TeamView> views = new CopyOnWriteArrayList<TeamView>();

    private String name;
    protected static final String JOBS_FOLDER_NAME = "jobs";
    private String description;
    private transient TeamManager teamManager;
    private String customFolderName;
    
    /**
     * List of nodes known to this team but are disabled.
     * When nodes are disabled, jobs can not be scheduled 
     * on those nodes
     */
    protected Set<String> enabledVisibleNodes = new HashSet<String>();

    //Used for unmarshalling
    Team() {
    }

    Team(String name, TeamManager teamManager) {
        this(name, name, teamManager);
    }

    Team(String teamName, String description, TeamManager teamManager) {
        this(teamName, description, null, teamManager);
    }

    Team(String teamName, String description, String customFolderName, TeamManager teamManager) {
        this.name = teamName;
        this.description = description;
        this.teamManager = teamManager;
        this.customFolderName = customFolderName;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    void setDescription(String description) throws IOException {
        this.description = description;
        getTeamManager().save();
    }

    String getCustomFolderName() {
        return customFolderName;
    }

    void setCustomFolderName(String customTeamFolderName) {
        this.customFolderName = customTeamFolderName;
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
            TeamMember member = findMember(userName);
            if (member != null) {
                isAdmin = member.isTeamAdmin();
            }
        }
        return isAdmin;
    }

    public List<TeamMember> getMembers() {
        return Collections.unmodifiableList(teamMembers);
    }

    public List<TeamJob> getJobs() {
        return Collections.unmodifiableList(jobs);
    }

    public List<TeamNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public List<TeamView> getViews() {
        return Collections.unmodifiableList(views);
    }

    public Set<String> getJobNames() {
        Set<String> jobNames = new TreeSet<String>();
        for (TeamJob job : getJobs()) {
            jobNames.add(job.getId());
        }
        return jobNames;
    }
    
    public Set<String> getViewNames() {
        Set<String> viewNames = new TreeSet<String>();
        for (TeamView view : getViews()) {
            viewNames.add(view.getId());
        }
        return viewNames;
    }
    
     public Set<String> getNodeNames() {
        Set<String> nodeNames = new TreeSet<String>();
        for (TeamNode node : getNodes()) {
            nodeNames.add(node.getId());
        }
        return nodeNames;
    }

    public TeamMember findMember(String userName) {
        for (TeamMember member : teamMembers) {
            if (userName.equalsIgnoreCase(member.getName())) {
                return member;
            }
        }
        return null;
    }

    void addMember(String teamMemberSid, boolean isTeamAdmin, boolean canCreate,
            boolean canDelete, boolean canConfigure, boolean canBuild,
            boolean canCreateNode, boolean canDeleteNode, boolean canConfigureNode,
            boolean canCreateView, boolean canDeleteView, boolean canConfigureView) throws IOException {
        TeamMember newMember = new TeamMember();
        newMember.setName(teamMemberSid);
        newMember.setAsTeamAdmin(isTeamAdmin);
        if (canCreate) {
            newMember.addPermission(Item.CREATE);
            newMember.addPermission(Item.EXTENDED_READ);
        }
        if (canDelete) {
            newMember.addPermission(Item.DELETE);
            newMember.addPermission(Item.WIPEOUT);
        }
        if (canConfigure) {
            newMember.addPermission(Item.CONFIGURE);
            newMember.addPermission(Item.EXTENDED_READ);
        }
        if (canBuild) {
            newMember.addPermission(Item.BUILD);
        }
        if (canCreateNode) {
            newMember.addPermission(Computer.CREATE);
        }
        if (canDeleteNode) {
            newMember.addPermission(Computer.DELETE);
        }
        if (canConfigureNode) {
            newMember.addPermission(Computer.CONFIGURE);
        }
        if (canCreateView) {
            newMember.addPermission(View.CREATE);
        }
        if (canDeleteView) {
            newMember.addPermission(View.DELETE);
        }
        if (canConfigureView) {
            newMember.addPermission(View.CONFIGURE);
        }
        newMember.addPermission(Item.READ);
        newMember.addPermission(Item.WORKSPACE);
        addMember(newMember);
    }

    void updateMember(String teamMemberSid, boolean isTeamAdmin, boolean canCreate,
            boolean canDelete, boolean canConfigure, boolean canBuild,
            boolean canCreateNode, boolean canDeleteNode, boolean canConfigureNode,
            boolean canCreateView, boolean canDeleteView, boolean canConfigureView) throws IOException {
        TeamMember currentMember = findMember(teamMemberSid);
        if (currentMember != null) {
            currentMember.setAsTeamAdmin(isTeamAdmin);
            if (canCreate) {
                currentMember.addPermission(Item.CREATE);
                currentMember.addPermission(Item.EXTENDED_READ);
            } else {
                currentMember.removePermission(Item.CREATE);
                if (!canConfigure) {
                    currentMember.removePermission(Item.EXTENDED_READ);
                }
            }
            if (canDelete) {
                currentMember.addPermission(Item.DELETE);
                currentMember.addPermission(Item.WIPEOUT);
            } else {
                currentMember.removePermission(Item.DELETE);
                currentMember.removePermission(Item.WIPEOUT);
            }
            if (canConfigure) {
                currentMember.addPermission(Item.CONFIGURE);
                currentMember.addPermission(Item.EXTENDED_READ);
            } else {
                currentMember.removePermission(Item.CONFIGURE);
                if (!canCreate) {
                    currentMember.removePermission(Item.EXTENDED_READ);
                }
            }
            if (canBuild) {
                currentMember.addPermission(Item.BUILD);
            } else {
                currentMember.removePermission(Item.BUILD);
            }

            if (canCreateNode) {
                currentMember.addPermission(Computer.CREATE);
            } else {
                currentMember.removePermission(Computer.CREATE);
            }

            if (canDeleteNode) {
                currentMember.addPermission(Computer.DELETE);
            } else {
                currentMember.removePermission(Computer.DELETE);
            }
            
            if (canConfigureNode) {
                currentMember.addPermission(Computer.CONFIGURE);
            }else {
                currentMember.removePermission(Computer.CONFIGURE);
            }

            if (canCreateView) {
                currentMember.addPermission(View.CREATE);
            } else {
                currentMember.removePermission(View.CREATE);
            }

            if (canDeleteView) {
                currentMember.addPermission(View.DELETE);
            } else {
                currentMember.removePermission(View.DELETE);
            }
            
            if (canConfigureView) {
                currentMember.addPermission(View.CONFIGURE);
            }else {
                currentMember.removePermission(View.CONFIGURE);
            }
            
            getTeamManager().save();
        }
    }

    void addMember(TeamMember member) throws IOException {
        if (!teamMembers.contains(member)) {
            teamMembers.add(member);
            getTeamManager().save();
        }
    }

    void removeMember(String userName) throws IOException {
        TeamMember member = findMember(userName);
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
            return findMember(userName) != null;
        }
    }

    public TeamView findView(String viewName) {
        for (TeamView view : views) {
            if (viewName.equals(view.getId())) {
                return view;
            }
        }
        return null;
    }

    void addView(TeamView view) throws IOException {
        if (!views.contains(view)) {
            views.add(view);
            getTeamManager().save();
        }
    }

    boolean removeView(String viewName) throws IOException {
        for (TeamView view : views) {
            if (viewName.equals(view.getId())) {
                return removeView(view);
            }
        }
        return false;
    }

    boolean removeView(TeamView view) throws IOException {
        if (views.contains(view)) {
            if (views.remove(view)) {
                getTeamManager().save();
                return true;
            }
        }
        return false;
    }

    void renameView(String oldViewName, String newViewId) throws IOException {
        TeamView view = findView(oldViewName);
        if (view != null) {
            view.setId(newViewId);
            getTeamManager().save();
        }

    }

    public boolean isViewOwner(String viewName) {
        return findView(viewName) != null;
    }

    public TeamNode findNode(String nodeName) {
        for (TeamNode node : nodes) {
            if (nodeName.equals(node.getId())) {
                return node;
            }
        }
        return null;
    }

    void addNode(TeamNode node) throws IOException {
        if (!nodes.contains(node)) {
            nodes.add(node);
            getTeamManager().save();
        }
    }

    boolean removeNode(String nodeName) throws IOException {
        for (TeamNode node : nodes) {
            if (nodeName.equals(node.getId())) {
                return removeNode(node);
            }
        }
        return false;
    }

    boolean removeNode(TeamNode node) throws IOException {
        if (nodes.contains(node)) {
            if (nodes.remove(node)) {
                getTeamManager().save();
                return true;
            }
        }
        return false;
    }

    void renameNode(String oldNodeName, String newNodeId) throws IOException {
        TeamNode node = findNode(oldNodeName);
        if (node != null) {
            node.setId(newNodeId);
            getTeamManager().save();
        }

    }

    public boolean isNodeOwner(String nodeName) {
        return findNode(nodeName) != null;
    }

    public TeamJob findJob(String jobName) {
        for (TeamJob job : jobs) {
            if (jobName.equals(job.getId())) {
                return job;
            }
        }
        return null;
    }

    void addJob(TeamJob job) throws IOException {
        addJob(job, true);
    }

    void addJob(TeamJob job, boolean save) throws IOException {
        if (!jobs.contains(job)) {
            jobs.add(job);
            if (save) {
                getTeamManager().save();
            }
        }
    }

    boolean removeJob(String jobName) throws IOException {
        for (TeamJob job : jobs) {
            if (jobName.equals(job.getId())) {
                return removeJob(job);
            }
        }
        return false;
    }

    boolean removeJob(TeamJob job) throws IOException {
        if (jobs.contains(job)) {
            if (jobs.remove(job)) {
                getTeamManager().save();
                return true;
            }
        }
        return false;
    }

    public boolean isJobOwner(String jobName) {
        return findJob(jobName) != null;
    }

    void renameJob(String oldJobName, String newJobId) throws IOException {
        TeamJob job = findJob(oldJobName);
        if (job != null) {
            job.setId(newJobId);
            getTeamManager().save();
        }

    }

    List<File> getJobsRootFolders(File teamsFolder) {
        return getJobsRootFolders(teamsFolder, false);
    }

    List<File> getJobsRootFolders(File teamsFolder, final boolean initializingTeam) {
        // if !initializingTeam make sure jobs on disk with no entries in teams.xml are ignored
        File jobsFolder = getJobsFolder(teamsFolder);
        if (jobsFolder.exists()) {
            final Set<String> jobNames = new HashSet<String>();;
            if (!initializingTeam) {
                for (TeamJob job : jobs) {
                    jobNames.add(job.getId());
                }
            }
            File[] jobsRootFolders = jobsFolder.listFiles(new FileFilter() {
                @Override
                public boolean accept(File child) {
                    if (!initializingTeam && !jobNames.contains(child.getName())) {
                        return false;
                    }
                    return child.isDirectory() && Items.getConfigFile(child).exists();
                }
            });
            if (jobsRootFolders != null) {
                return Arrays.asList(jobsRootFolders);
            }
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Return the team folder.
     *
     * @param teamsFolder the outer "teams" folder, or for public, hudson home
     * @return team folder that will contain "jobs" folder
     */
    File getTeamFolder(File teamsFolder) {
        if (PUBLIC_TEAM_NAME.equals(name)) {
            return teamsFolder;
        }
        if ((customFolderName != null) && !"".equals(customFolderName.trim())) {
            return new File(customFolderName);
        }
        return new File(teamsFolder, name);
    }

    /**
     * The folder where all the jobs of this team are saved
     *
     * @return File
     */
    File getJobsFolder(File teamsFolder) {
        return new File(getTeamFolder(teamsFolder), JOBS_FOLDER_NAME);
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
            @Override
            public boolean hasPermission(Authentication a, Permission permission) {
                return false;
            }
        };
    }
    
    void addToEnabledVisibleNodes(String nodeName) throws IOException{
        enabledVisibleNodes.add(nodeName);
        getTeamManager().save();
    }
    
    void removeFromEnabledVisibleNodes(String nodeName) throws IOException{
        enabledVisibleNodes.remove(nodeName);
        getTeamManager().save();
    }
    
    // Called from jelly also
    public boolean isVisibleNodeEnabled(String nodeName){
        return enabledVisibleNodes.contains(nodeName);
    }
    
    // Used in Jelly
    public List<TeamJob> getVisibleJobs(){
        List<TeamJob> visibleJobs = new ArrayList<TeamJob>();
        for (Team team : getTeamManager().getTeams().values()) {
            for (TeamJob teamJob : team.getJobs()) {
                if (teamJob.isVisible(name)){
                   visibleJobs.add(teamJob);
                }
            }
        }
        return visibleJobs;
    }
    
    // Used in Jelly
    public List<TeamNode> getVisibleNodes(){
        List<TeamNode> visibleNodes = new ArrayList<TeamNode>();
        for (Team team : getTeamManager().getTeams().values()) {
            for (TeamNode teamNode : team.getNodes()) {
                if (teamNode.isVisible(name)){
                   visibleNodes.add(teamNode);
                }
            }
        }
        return visibleNodes;
    }
    
    // Used in Jelly
    public List<TeamView> getVisibleViews(){
        List<TeamView> visibleViews = new ArrayList<TeamView>(); 
        for (Team team : getTeamManager().getTeams().values()) {
            for (TeamView teamView : team.getViews()) {
                if (teamView.isVisible(name)){
                   visibleViews.add(teamView);
                }
            }
        }
        return visibleViews;
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
    // used in the jelly
    public TeamManager getTeamManager() {
        if (teamManager == null) {
            return HudsonSecurityEntitiesHolder.getHudsonSecurityManager().getTeamManager();
        } else {
            return teamManager;
        }
    }

    public static class ConverterImpl implements Converter {

        @Override
        public boolean canConvert(Class type) {
            return type == Team.class;
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            Team team = (Team) source;
            writer.startNode("name");
            writer.setValue(team.getName());
            writer.endNode();
            if ((team.getDescription() != null) && !"".equals(team.getDescription().trim())) {
                writer.startNode("description");
                writer.setValue(team.getDescription());
                writer.endNode();
            }
            if ((team.getCustomFolderName() != null) && !"".equals(team.getCustomFolderName().trim())) {
                writer.startNode("customFolderName");
                writer.setValue(team.getCustomFolderName());
                writer.endNode();
            }

            for (TeamJob job : team.getJobs()) {
                writer.startNode("job");
                context.convertAnother(job);
                writer.endNode();
            }
            for (TeamNode node : team.getNodes()) {
                writer.startNode("node");
                context.convertAnother(node);
                writer.endNode();
            }
            for (TeamView view : team.getViews()) {
                writer.startNode("view");
                context.convertAnother(view);
                writer.endNode();
            }
            for (TeamMember member : team.getMembers()) {
                writer.startNode("member");
                context.convertAnother(member);
                writer.endNode();
            }
            StringWriter strWriter = new StringWriter();
            if (team.enabledVisibleNodes.size() > 0) {
                for (String nodeName : team.enabledVisibleNodes) {
                    strWriter.append(nodeName);
                    strWriter.append(",");
                }
                writer.startNode("enabledNodes");
                writer.setValue(strWriter.toString());
                writer.endNode();
            }
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            Team team = new Team();
            while (reader.hasMoreChildren()) {
                reader.moveDown();

                if ("name".equals(reader.getNodeName())) {
                    team.name = reader.getValue();
                }
                if ("description".equals(reader.getNodeName())) {
                    team.description = reader.getValue();
                }

                if ("customFolderName".equals(reader.getNodeName())) {
                    team.customFolderName = reader.getValue();
                }

                if ("job".equals(reader.getNodeName())) {
                    TeamJob teamJob = (TeamJob) uc.convertAnother(team, TeamJob.class);
                    team.jobs.add(teamJob);
                } else if ("node".equals(reader.getNodeName())) {
                    TeamNode teamSlave = (TeamNode) uc.convertAnother(team, TeamNode.class);
                    team.nodes.add(teamSlave);
                } else if ("view".equals(reader.getNodeName())) {
                    TeamView teamView = (TeamView) uc.convertAnother(team, TeamView.class);
                    team.views.add(teamView);
                } else if ("member".equals(reader.getNodeName())) {
                    TeamMember teamMember = (TeamMember) uc.convertAnother(team, TeamMember.class);
                    team.teamMembers.add(teamMember);
                }else if ("enabledNodes".equals(reader.getNodeName())) {
                    String nodeNames = reader.getValue();
                    team.enabledVisibleNodes.addAll(Arrays.asList(nodeNames.split(",")));
                }

                reader.moveUp();
            }
            return team;
        }
    }
}
