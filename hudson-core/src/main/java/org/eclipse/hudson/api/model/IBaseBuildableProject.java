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
 *    Winston Prakash, Nikita Levyankov
 *
 *******************************************************************************/

package org.eclipse.hudson.api.model;

import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.tasks.BuildStep;
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
     * Adds a new {@link hudson.tasks.BuildStep}, builder, to this
     * {@link IBaseBuildableProject} and saves the configuration.
     *
     * @param builder builder.
     * @throws java.io.IOException exception.
     */
    public void addBuilder(Builder builder) throws IOException;

    /**
     * Removes a {@link BuildStep} builder from this project, if it's active.
     *
     * @param builder builder.
     * @throws java.io.IOException exception.
     */
    public void removeBuilder(Descriptor<Builder> builder) throws IOException;
    
    public Builder getBuilder(Descriptor<Builder> descriptor);
    
    /**
     * @return list of project {@link hudson.tasks.Builder}
     */
    List<Builder> getBuilders();
    
    void setBuilders(DescribableList<Builder, Descriptor<Builder>> builders);

    DescribableList<Builder, Descriptor<Builder>> getBuildersList();

    /**
     * Adds a new {@link BuildStep} to this {@link Project} and saves the
     * configuration.
     *
     * @param buildWrapper buildWrapper.
     * @throws java.io.IOException exception.
     */
    public void addBuildWrapper(BuildWrapper buildWrapper) throws IOException;
    
     /**
     * Removes a buildWrapper from this project, if it's active.
     *
     * @param buildWrapper buildWrapper.
     * @throws java.io.IOException exception.
     */
    public void removeBuildWrapper(Descriptor<BuildWrapper> buildWrapper) throws IOException;
    
    public BuildWrapper getBuildWrapper(Descriptor<BuildWrapper> descriptor);
    /**
     * @inheritDoc
     */
    public Map<Descriptor<BuildWrapper>, BuildWrapper> getBuildWrappers();
    
     /**
     * @inheritDoc
     * 
     */
    public void setBuildWrappers(DescribableList<BuildWrapper, Descriptor<BuildWrapper>> buildWrappers);
    /**
     * @inheritDoc
     * 
     * @deprecated as of 2.2.0 don't use this field directly. Use other methods such as getBuildWrappers, addBuildWrapper & removeBuildWrapper
     */
    @Deprecated
    public DescribableList<BuildWrapper, Descriptor<BuildWrapper>> getBuildWrappersList();

    /**
     * Adds a new {@link hudson.tasks.BuildStep} to this
     * {@link IBaseBuildableProject} and saves the configuration.
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
    

    Publisher getPublisher(Descriptor<Publisher> descriptor);
    
    /**
     * @return map of project {@link hudson.tasks.Publisher}
     */
    Map<Descriptor<Publisher>, Publisher> getPublishers();
    
    public void setPublishers(DescribableList<Publisher, Descriptor<Publisher>> publishers);

    /**
     * Returns the list of the publishers available in the hudson.
     *
     * @return the list of the publishers available in the hudson.
     * * @deprecated as of 2.2.0 do not use this field directly. Use other methods such as getPublishers, addPublisher & removePublisher
     */
    @Deprecated
    public DescribableList<Publisher, Descriptor<Publisher>> getPublishersList();
}
