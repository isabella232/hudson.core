
/*******************************************************************************
 *
 * Copyright (c) 2004-2010 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    Nikita Levyankov, Anton Kozak
 *
 *******************************************************************************/

hudsonRules["A.reset-button"] = function(e) {
    e.onclick = function() {
        new Ajax.Request(this.getAttribute("resetURL"), {
                method : 'get',
                onSuccess : function(x) {
                    location.reload(true);
                },
                onFailure : function(x) {

                }
            });
        return false;
    }
    e.tabIndex = 9999; // make help link unnavigable from keyboard
    e = null; // avoid memory leak
}

jQuery(document).ready(function(){
    jQuery('select[name=cascadingProjectName]').change(function() {
        var url = window.location.href;
        var jobUrl = url.substr(0, url.lastIndexOf('/'))+'/updateCascadingProject';
        var cascadingProject = jQuery(this).val();
        new Ajax.Request(jobUrl+'?projectName='+cascadingProject, {
            method : 'get',
            onSuccess : function(x) {
                location.reload(true);
            }
        });
   });
});