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

import hudson.model.Descriptor;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Interface for {@link hudson.model.BaseBuildableProject}.
 * <p/>
 * Date: 11/25/11
 *
 * @author Nikita Levyankov
 */
public interface IBaseBuildableProject extends IAbstractProject {
    /**
     * @return list of project {@link hudson.tasks.Builder}
     */
    List<Builder> getBuilders();

    DescribableList<Builder, Descriptor<Builder>> getBuildersList();

    void setBuilders(DescribableList<Builder, Descriptor<Builder>> builders);

    /**
     * @return map of project {@link hudson.tasks.BuildWrapper}
     */
    Map<Descriptor<BuildWrapper>, BuildWrapper> getBuildWrappers();

    /**
     * @return map of project {@link hudson.tasks.Publisher}
     */
    Map<Descriptor<Publisher>, Publisher> getPublishers();

    Publisher getPublisher(Descriptor<Publisher> descriptor);

    /**
     * Adds a new {@link hudson.tasks.BuildStep} to this {@link IBaseBuildableProject} and saves the configuration.
     *
     * @param publisher publisher.
     * @throws java.io.IOException exception.
     */
    void addPublisher(Publisher publisher) throws IOException;

    /**
     * Removes a publisher from this project, if it's active.
     *
     * @param publisher publisher.
     * @throws java.io.IOException exception.
     */
    void removePublisher(Descriptor<Publisher> publisher) throws IOException;
}
