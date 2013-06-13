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
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * A simple model to hold team visibility information
 *
 * @since 3.1.0
 * @author Winston Prakash
 */
public class TeamJob {

    private String id;
    private Set<String> visibleToTeams = new HashSet<String>();

    public TeamJob() {
    }

    public TeamJob(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    void addVisibility(String teamName) {
        if (!visibleToTeams.contains(teamName)) {
            visibleToTeams.add(teamName);
        }
    }

    void removeVisibility(String teamName) {
        if (visibleToTeams.contains(teamName)) {
            visibleToTeams.remove(teamName);
        }
    }

    void removeAllVisibilities() {
        visibleToTeams.clear();
    }

    Set<String> getVisiblities() {
        return visibleToTeams;
    }

    public String getVisiblitiesAsString() {
        if (!visibleToTeams.isEmpty()) {
            StringWriter strWriter = new StringWriter();
            for (String teamName : visibleToTeams) {
                strWriter.append(teamName);
                strWriter.append(":");
            }
            return strWriter.toString();
        }
        return "";
    }

    public Boolean isVisible(String name) {
        return visibleToTeams.contains(name);
    }

    public static class ConverterImpl implements Converter {

        @Override
        public boolean canConvert(Class type) {
            return type == TeamJob.class;
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            TeamJob teamJob = (TeamJob) source;
            writer.startNode("id");
            writer.setValue(teamJob.id);
            writer.endNode();
            StringWriter strWriter = new StringWriter();

            if (teamJob.visibleToTeams.size() > 0) {
                for (String teamName : teamJob.visibleToTeams) {
                    strWriter.append(teamName);
                    strWriter.append(",");
                }
                writer.startNode("visibility");
                writer.setValue(strWriter.toString());
                writer.endNode();
            }
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            TeamJob teamJob = new TeamJob();
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                if ("id".equals(reader.getNodeName())) {
                    teamJob.id = reader.getValue();
                }
                if ("visibility".equals(reader.getNodeName())) {
                    String teamNames = reader.getValue();
                    for (String teamName : teamNames.split(",")) {
                        teamJob.visibleToTeams.add(teamName);
                    }
                }
                reader.moveUp();
            }
            return teamJob;
        }
    }
}
