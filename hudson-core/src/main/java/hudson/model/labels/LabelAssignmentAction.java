/*******************************************************************************
 *
 * Copyright (c) 2013 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Kohsuke Kawaguchi
 *******************************************************************************/ 
package hudson.model.labels;

import hudson.model.Action;
import hudson.model.Label;
import hudson.model.queue.SubTask;

public interface LabelAssignmentAction extends Action {
    Label getAssignedLabel(SubTask task);
}
