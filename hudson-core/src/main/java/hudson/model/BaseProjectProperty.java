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

package hudson.model;

import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.hudson.api.model.IJob;
import org.eclipse.hudson.api.model.IProjectProperty;

/**
 * Base property implementation. Contains common methods for setting and getting cascading and overridden properties.
 * <p/>
 * Date: 9/22/11
 *
 * @author Nikita Levyankov
 */
public class BaseProjectProperty<T> implements IProjectProperty<T> {

    private transient String propertyKey;
    private transient IJob job;
    private T originalValue;
    private boolean propertyOverridden;

    /**
     * Instantiate new property.
     *
     * @param job owner of current property.
     */
    public BaseProjectProperty(IJob job) {
        setJob(job);
    }

    /**
     * {@inheritDoc}
     */
    public void setKey(String propertyKey) {
        this.propertyKey = propertyKey;
    }

    /**
     * {@inheritDoc}
     */
    public void setJob(IJob job) {
        assert job != null;
        this.job = job;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public T getCascadingValue() {
        return job.hasCascadingProject() ?
            (T) job.getCascadingProject().getProperty(propertyKey, this.getClass()).getValue() : getDefaultValue();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPropertyOverridden() {
        return propertyOverridden;
    }

    /**
     * {@inheritDoc}
     */
    public T getDefaultValue() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public T getValue() {
        if (isPropertyOverridden() || null != originalValue) {
            return getOriginalValue();
        }
        return getCascadingValue();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void setValue(T value) {
        value = prepareValue(value);
        if (!job.hasCascadingProject()) {
            originalValue = value;
        } else if (allowOverrideValue(
            (T) job.getCascadingProject().getProperty(propertyKey, this.getClass()).getValue(), value)) {
            originalValue = value;
            propertyOverridden = true;
        } else {
            this.originalValue = null;
            propertyOverridden = false;
        }
    }

    /**
     * Returns true, if cascading value should be overridden by candidate value.
     *
     * @param cascadingValue value from cascading project if any.
     * @param candidateValue candidate value.
     * @return true if cascading value should be replaced by candidate value.
     */
    protected boolean allowOverrideValue(T cascadingValue, T candidateValue) {
        return !ObjectUtils.equals(cascadingValue, candidateValue);
    }

    /**
     * Pre-process candidate value.
     *
     * @param candidateValue candidateValue.
     * @return candidateValue by default.
     */
    protected T prepareValue(T candidateValue) {
        return candidateValue;
    }

    /**
     * {@inheritDoc}
     */
    public T getOriginalValue() {
        return originalValue;
    }
}
