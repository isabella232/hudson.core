/*******************************************************************************
 *
 * Copyright (c) 2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    Nikita Levyankov
 *
 *******************************************************************************/

package org.eclipse.hudson.api.model;

import hudson.model.Job;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Job interface that exposes cascading functionality
 * <p/>
 * Date: 11/25/11
 *
 * @author Nikita Levyankov
 */
public interface ICascadingJob<T extends Job<?, ?>> extends IJob<T> {

    /**
     * Returns cascading project name.
     *
     * @return cascading project name.
     */
    String getCascadingProjectName();

    /**
     * Sets cascadingProject name and saves project configuration.
     *
     * @param cascadingProjectName cascadingProject name.
     * @throws java.io.IOException if configuration couldn't be saved.
     */
    void setCascadingProjectName(String cascadingProjectName) throws IOException;

    /**
     * Returns selected cascading project.
     *
     * @return cascading project.
     */
    ICascadingJob getCascadingProject();

    /**
     * Returns job property by specified key.
     *
     * @param key key.
     * @param clazz IProperty subclass.
     * @return {@link IProjectProperty} instance or null.
     */
    IProjectProperty getProperty(String key, Class<? extends IProjectProperty> clazz);

    /**
     * Returns job property by specified key.
     *
     * @param key key.
     * @return {@link org.eclipse.hudson.api.model.IProjectProperty} instance or
     * null.
     */
    IProjectProperty getProperty(String key);

    /**
     * Removes project property.
     *
     * @param key property key.
     */
    void removeProjectProperty(String key);

    /**
     * Put job property to properties map.
     *
     * @param key key.
     * @param property property instance.
     */
    void putProjectProperty(String key, IProjectProperty property);

    /**
     * @return project properties.
     */
    Map<String, IProjectProperty> getProjectProperties();

    /**
     * Checks whether current job is inherited from other project.
     *
     * @return boolean.
     */
    boolean hasCascadingProject();

    /**
     * Remove cascading child project name and saves job configuration
     *
     * @param oldChildName old child project name.
     * @param newChildName new child project name.
     * @throws java.io.IOException if configuration couldn't be saved.
     */
    void renameCascadingChildName(String oldChildName, String newChildName) throws IOException;

    /**
     * Checks whether job has cascading children with given name
     *
     * @param cascadingChildName name of child.
     * @return true if job has child with specified name, false - otherwise.
     */
    boolean hasCascadingChild(String cascadingChildName);

    /**
     * Remove cascading child project name and saves job configuration
     *
     * @param cascadingChildName cascading child project name.
     * @throws java.io.IOException if configuration couldn't be saved.
     */
    void removeCascadingChild(String cascadingChildName) throws IOException;

    /**
     * Adds cascading child project name and saves configuration.
     *
     * @param cascadingChildName cascading child project name.
     * @throws java.io.IOException if configuration couldn't be saved.
     */
    void addCascadingChild(String cascadingChildName) throws IOException;

    /**
     * @return list of cascading children project names.
     */
    Set<String> getCascadingChildrenNames();

    /**
     * Renames cascading project name. For the properties processing and
     * children links updating please use {@link #setCascadingProjectName}
     * instead.
     *
     * @param cascadingProjectName new project name.
     * @throws java.io.IOException
     */
    void renameCascadingProjectNameTo(String cascadingProjectName) throws IOException;
}
