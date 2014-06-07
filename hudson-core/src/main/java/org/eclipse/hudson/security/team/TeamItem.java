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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A team item that has team based isolation information.
 *
 * @since 3.2.0
 * @author Winston Prakash
 */
public class TeamItem {

    private String id;
    /**
     * When visibility is set, then those teams can use this slave to build
     * their jobs
     */
    protected Set<String> visibilityToTeams = new HashSet<String>();

    private transient boolean moveAllowed = true;

    public TeamItem() {
    }

    public TeamItem(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    void addVisibility(String teamName) {
        if (!visibilityToTeams.contains(teamName)) {
            visibilityToTeams.add(teamName);
        }
    }

    void removeVisibility(String teamName) {
        if (visibilityToTeams.contains(teamName)) {
            visibilityToTeams.remove(teamName);
        }
    }

    void removeAllVisibilities() {
        visibilityToTeams.clear();
    }

    Set<String> getVisiblities() {
        return visibilityToTeams;
    }

    public String getVisiblitiesAsString() {
        if (!visibilityToTeams.isEmpty()) {
            StringWriter strWriter = new StringWriter();
            Iterator iterator = visibilityToTeams.iterator();
            while (iterator.hasNext()) {
                strWriter.append((String) iterator.next());
                if (iterator.hasNext()) {
                    strWriter.append(":");
                };
            }
            return strWriter.toString();
        }
        return "";
    }

    public Boolean isVisible(String name) {
        return visibilityToTeams.contains(name);
    }

    public boolean isMoveAllowed() {
        return moveAllowed;
    }

    public void setMoveAllowed(boolean moveAllowed) {
        this.moveAllowed = moveAllowed;
    }

    public static class ConverterImpl implements Converter {

        @Override
        public boolean canConvert(Class type) {
            return type == TeamItem.class;
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            TeamItem teamItem = (TeamItem) source;
            writer.startNode("id");
            writer.setValue(teamItem.id);
            writer.endNode();
            StringWriter strWriter = new StringWriter();

            if (teamItem.visibilityToTeams.size() > 0) {
                for (String teamName : teamItem.visibilityToTeams) {
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
            TeamItem teamItem = new TeamItem();
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                if ("id".equals(reader.getNodeName())) {
                    teamItem.id = reader.getValue();
                }
                if ("visibility".equals(reader.getNodeName())) {
                    String teamNames = reader.getValue();
                    teamItem.visibilityToTeams.addAll(Arrays.asList(teamNames.split(",")));
                }
                reader.moveUp();
            }
            return teamItem;
        }
    }
}
