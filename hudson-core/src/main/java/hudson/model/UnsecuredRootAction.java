/*
 * Copyright (c) 2014 Oracle Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    WInston Prakash - initial API and implementation and/or initial documentation
 */

package hudson.model;

import hudson.ExtensionPoint;

/**
 * This {@link RootAction} is meant to give access to requests outside of Hudson security envelop {@link Hudson}.
 * Extreme caution must be exercised to extend a root action with this ExtensonPoint.
 * Though this action does not require the minimal READ permission, This is not an "insecure" action that compromises 
 * security. So by default it is turned off. The Admin must explicitly allow this action via system settings.
 * 
 * @author Winston Prakash
 * @since 3.2.0
 */
public interface UnsecuredRootAction extends RootAction, ExtensionPoint{
    
}
