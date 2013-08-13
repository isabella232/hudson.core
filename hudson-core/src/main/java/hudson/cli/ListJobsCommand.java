/*
 * Copyright (c) 2013 Hudson.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Hudson - initial API and implementation and/or initial documentation
 */

package hudson.cli;

import hudson.Extension;
import static hudson.cli.ListTeamsCommand.Format.XML;
import static hudson.cli.UpdateJobCommand.validateTeam;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.TopLevelItem;
import hudson.util.QuotedStringTokenizer;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.eclipse.hudson.security.HudsonSecurityManager;
import org.eclipse.hudson.security.team.Team;
import org.eclipse.hudson.security.team.TeamManager;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.springframework.security.Authentication;

/**
 * List the jobs in Hudson.
 * <p>
 * If team management is enabled, list the jobs by team.
 * 
 * @author Bob Foster
 */
@Extension
public class ListJobsCommand extends CLICommand {
    @Override
    public String getShortDescription() {
        return "Lists the jobs in Hudson";
    }

    private String getCurrentUser() {
        Authentication authentication = HudsonSecurityManager.getAuthentication();
        return authentication.getName();
    }

    enum Format {
        XML, CSV, PLAIN
    }
    @Argument(metaVar = "TEAM", usage = "Team to list; if omitted, all visible teams.", required = false)
    public String team;
    @Option(name = "-format", usage = "Controls how the output from this command is printed.")
    public ListTeamsCommand.Format format = ListTeamsCommand.Format.PLAIN;

    TeamManager teamManager;
    
    @Override
    protected int run() throws TeamManager.TeamNotFoundException {
        Team targetTeam = validateReadAccessToTeam(team, stderr);

        if (team != null && targetTeam == null) {
            return -1;
        }

        teamManager = Hudson.getInstance().getTeamManager();
        String[] jobs = null;
        if (targetTeam != null) {
            Set<String> aTeamJobs = targetTeam.getJobNames();
            Arrays.sort(jobs = aTeamJobs.toArray(new String[aTeamJobs.size()]));
        } else {
            // Get items user can READ
            List<TopLevelItem> items = Hudson.getInstance().getItems();
            List<String> itemNames = new ArrayList<String>();
            for (TopLevelItem item : items) {
                if (item instanceof Job) {
                    itemNames.add(item.getName());
                }
            }
            Arrays.sort(jobs = itemNames.toArray(new String[itemNames.size()]));
        }
        switch (format) {
            case XML:
                PrintWriter w = new PrintWriter(stdout);
                w.println("<jobs>");
                for (String job : jobs) {
                    w.print("  <job>");
                    w.print(job);
                    w.println("  </job>");
                }
                w.println("</jobs>");
                w.flush();
                break;
            case CSV:
            case PLAIN:
                for (String job : jobs) {
                    stdout.println(job);
                }
                break;
        }
        return 0;
    }
    
    public static Team validateReadAccessToTeam(String team, PrintStream stderr) {
        Hudson h = Hudson.getInstance();
        TeamManager teamManager = h.getTeamManager();
        Team targetTeam = null;
        if (team != null) {
            if (!teamManager.isTeamManagementEnabled()) {
                stderr.println("team may not be specified unless team management is enabled");
            } else {
                try {
                    // check team exists first for better error message
                    targetTeam = teamManager.findTeam(team);
                    if (!team.equals(Team.PUBLIC_TEAM_NAME) && !teamManager.getCurrentUserTeams().contains(team)) {
                        stderr.println("Current user does not have read access to team "+team);
                        targetTeam = null;
                    }
                } catch (TeamManager.TeamNotFoundException e) {
                    stderr.println("Team "+team+" does not exist");
                }
            }
        }
        return targetTeam;
    }
}
