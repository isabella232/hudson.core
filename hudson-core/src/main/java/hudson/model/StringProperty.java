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

import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.hudson.api.model.IJob;
import org.eclipse.hudson.api.model.IProperty;

/**
 * String property.
 * <p/>
 * Date: 9/22/11
 *
 * @author Nikita Levyankov
 */
public class StringProperty implements IProperty<String> {

    private Enum propertyKey;
    private transient IJob job;
    private String originalValue;
    private boolean propertyOverridden;

    public void setKey(Enum propertyKey) {
        this.propertyKey = propertyKey;
    }

    public void setJob(IJob job) {
        this.job = job;
    }

    public StringProperty() {
    }

    public void setValue(String value) throws IOException {
        value = StringUtils.trimToNull(value);
        if (!job.hasCascadingProject()) {
            originalValue = value;
        } else if (!StringUtils.equalsIgnoreCase(
            (String) job.getCascadingProject().getProperty(propertyKey, this.getClass()).getValue(), value)) {
            originalValue = value;
            propertyOverridden = true;
        } else {
            this.originalValue = null;
            propertyOverridden = false;
        }
    }

    public String getOriginalValue() {
        return originalValue;
    }

    public String getCascadingValue() throws IOException {
        return job.hasCascadingProject() ?
            (String) job.getCascadingProject().getProperty(propertyKey, this.getClass()).getValue() : null;
    }

    public boolean isPropertyOverridden() {
        return propertyOverridden;
    }

    public String getValue() throws IOException {
        if (isPropertyOverridden() || null != getOriginalValue()) {
            return getOriginalValue();
        }
        return getCascadingValue();
    }
}
