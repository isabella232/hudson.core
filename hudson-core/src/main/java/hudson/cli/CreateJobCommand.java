/*******************************************************************************
 *
 * Copyright (c) 2004-2010, Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *
 *
 *
 *******************************************************************************/ 

package hudson.cli;

import hudson.model.Hudson;
import hudson.Extension;
import static hudson.cli.UpdateJobCommand.validateTeam;
import hudson.model.Failure;
import hudson.model.Item;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import org.eclipse.hudson.security.team.Team;
import org.eclipse.hudson.security.team.TeamManager;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 * Creates a new job by reading stdin as a configuration XML file.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class CreateJobCommand extends CLICommand {

    @Override
    public String getShortDescription() {
        return "Creates a new job by reading stdin or file as a configuration XML file";
    }
    @Argument(metaVar = "NAME", usage = "Name of the job to create. The job name should not be team  qualified. Ex: job1.", required = true)
    public String name;
    @Argument(metaVar = "TEAM", usage = "Team to create the job in. Optional.", index = 1, required = false)
    public String team;
    @Option(name = "-file", usage = "Read config.xml from file rather than standard input.")
    public String file;

    protected int run() throws Exception {
        Hudson h = Hudson.getInstance();
        h.checkPermission(Item.CREATE);
        Team targetTeam = validateTeam(team, true, stderr);

        if (team != null && targetTeam == null) {
            return -1;
        }
        
        name = name.trim();
        if (!isGoodName(name, stderr)) {
            return -1;
        }
            
        String qualifiedJobName = targetTeam == null
                ? name
                : h.getTeamManager().getTeamQualifiedJobName(targetTeam, name);
        if (h.getItem(qualifiedJobName) != null) {
                stderr.println("Job '" + qualifiedJobName + "' already exists");
            return -1;
        }
        
        InputStream xml = stdin;
        if (file != null) {
            File inputFile = new File(file);
            if (!inputFile.exists()) {
                stderr.println("File '" + file + "' does not exist");
                return -1;
            }
            if (!inputFile.isFile()) {
                stderr.println("File '" + file + "' is not a file");
                return -1;
            }
            try {
                xml = new FileInputStream(inputFile);
            } catch (FileNotFoundException e) {
                stderr.println("File '" + file + "' not found");
                return -1;
            }
        }
        
        h.createProjectFromXML(name, team, xml);
        return 0;
    }
    
    public static boolean isGoodName(String name, PrintStream stderr) {
        if (Hudson.getInstance().isTeamManagementEnabled() && name.contains(TeamManager.TEAM_SEPARATOR)) {
            stderr.println("The job name cannot contain \"" + TeamManager.TEAM_SEPARATOR + "\" when team management is enabled.");
            return false;
        }
        
        try {
            Hudson.checkGoodName(name);
        } catch (Failure e) {
            stderr.println(e.getMessage());
            return false;
        }
        
        return true;
    }
}
