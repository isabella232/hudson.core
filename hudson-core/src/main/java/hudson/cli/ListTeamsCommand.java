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
import hudson.model.Hudson;
import hudson.util.QuotedStringTokenizer;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import org.eclipse.hudson.security.team.TeamManager;
import org.kohsuke.args4j.Option;

/**
 * Lists the teams and READ or CREATE job permissions of the current user.
 * @author Bob Foster
 * @since 3.1.0
 */
@Extension
public class ListTeamsCommand extends CLICommand {
    @Override
    public String getShortDescription() {
        return "Lists the teams and READ or CREATE job permissions of the current user";
    }

    enum Format {
        XML, CSV, PLAIN
    }
    @Option(name = "-format", usage = "Controls how the output from this command is printed.")
    public Format format = Format.PLAIN;

    @Override
    protected int run() throws TeamManager.TeamNotFoundException {
        TeamManager teamManager = Hudson.getInstance().getTeamManager();
        String[] teams = new String[0];
        if (teamManager.isTeamManagementEnabled()) {
            Collection<String> currentUserTeams = teamManager.getCurrentUserVisibleTeams();
            Arrays.sort(teams = currentUserTeams.toArray(new String[currentUserTeams.size()]));
        }
        switch (format) {
            case XML:
                PrintWriter w = new PrintWriter(stdout);
                w.println("<teams>");
                for (String team : teams) {
                    w.println("  <team>");
                    w.print("    <name>");
                    w.print(team);
                    w.println("</name>");
                    for (String permission : teamManager.getCurrentUserTeamPermissions(team)) {
                        w.print("    <permission>");
                        w.print(permission);
                        w.println("</permission>");
                    }
                    w.println("  </team>");
                }
                w.println("</teams>");
                w.flush();
                break;
            case CSV:
                stdout.printf("Team,%s,%s,%s,%s,%s,%s,%s,%s\n", TeamManager.ALL_TEAM_PERMISSIONS);
                for (String team : teams) {
                    stdout.printf(team+",%s,%s,%s,%s,%s,%s,%s,%s\n",
                        convertToXArray(teamManager.getCurrentUserTeamPermissions(team)));
                }
                break;
            case PLAIN:
                int big = 0;
                for (String team : teams) {
                    if (big < team.length()) {
                        big = team.length();
                    }
                }
                for (String team : teams) {
                    int len = team.length();
                    stdout.print(team);
                    pad(stdout, big-len+1);
                    String[] permissions = teamManager.getCurrentUserTeamPermissions(team);
                    for (int i = 0; i < permissions.length; i++) {
                        if (i == permissions.length - 1) {
                            stdout.println(permissions[i]);
                        } else {
                            stdout.print(permissions[i]);
                            stdout.print(" ");
                        }
                    }
                }
                break;
        }
        return 0;
    }
    
    private String[] convertToXArray(String[] currentUserTeamPermissions) {
        String[] allPermissions = TeamManager.ALL_TEAM_PERMISSIONS;
        // Both arrays are sorted, so it's just a merge
        String[] xarray = new String[allPermissions.length];
        int currentIndex = 0;
        for (int i = 0; i < allPermissions.length; i++) {
            if (currentIndex < currentUserTeamPermissions.length
                    && allPermissions[i].equals(currentUserTeamPermissions[currentIndex])) {
                xarray[i] = "X";
                currentIndex++;
            } else {
                xarray[i] = "-";
            }
        }
        return xarray;
    }

    private void pad(PrintStream out, int n) {
        while (n-- > 0) {
            out.print(" ");
        }
    }
    
}
