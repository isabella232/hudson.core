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

package hudson.util;

import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Saveable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Utility class for DescribableList logic.
 * <p/>
 * Date: 10/6/11
 *
 * @author Nikita Levyankov
 */
public final class DescribableListUtil {

    private DescribableListUtil() {
    }

    /**
     * Builds the list by creating a fresh instances from the submitted form.
     * <p/>
     * This method is almost always used by the owner.
     * This method does not invoke the save method.
     *
     * @param owner represents owner of {@link DescribableList}
     * @param req {@link StaplerRequest}
     * @param json Structured form data that includes the data for nested descriptor list.
     * @param descriptors list of descriptors to create instances from.
     * @return list.
     * @throws IOException              if any.
     * @throws Descriptor.FormException if any.
     */
    public static <T extends Describable<T>, D extends Descriptor<T>> DescribableList<T, D> buildFromJson(
        Saveable owner,
        StaplerRequest req,
        JSONObject json,
        List<D> descriptors)
        throws Descriptor.FormException, IOException {
        List<T> newList = new ArrayList<T>();

        for (Descriptor<T> d : descriptors) {
            String name = d.getJsonSafeClassName();
            if (json.has(name)) {
                newList.add(d.newInstance(req, json.getJSONObject(name)));
            }
        }
        return new DescribableList<T, D>(owner, newList);
    }

    /**
     * Rebuilds the list by creating a fresh instances from the submitted form.
     * <p/>
     * This version works with the the &lt;f:hetero-list> UI tag, where the user
     * is allowed to create multiple instances of the same descriptor. Order is also
     * significant.
     *
     * @param owner represents owner of {@link DescribableList}
     * @param req {@link StaplerRequest}
     * @param formData {@link JSONObject} populated based on form data,
     * @param key the JSON property name for 'formData' that represents the data for the list of {@link Describable}
     * @param descriptors list of descriptors to create instances from.
     * @return list.
     * @throws IOException              if any.
     * @throws Descriptor.FormException if any.
     * @see Descriptor#newInstancesFromHeteroList(org.kohsuke.stapler.StaplerRequest, net.sf.json.JSONObject, String, java.util.Collection)
     */
    public static <T extends Describable<T>, D extends Descriptor<T>> DescribableList<T, D> buildFromHetero(
        Saveable owner,
        StaplerRequest req, JSONObject formData,
        String key,
        Collection<D> descriptors)
        throws Descriptor.FormException, IOException {
        return new DescribableList<T, D>(owner, Descriptor.newInstancesFromHeteroList(req, formData, key, descriptors));
    }

}
