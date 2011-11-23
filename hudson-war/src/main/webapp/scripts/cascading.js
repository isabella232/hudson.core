
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

function getJobUrl() {
    var url = window.location.href;
    return url.substr(0, url.lastIndexOf('/'))
}

function onCascadingProjectUpdated() {
   jQuery('select[name=cascadingProjectName]').change(function() {
        var jobUrl = getJobUrl()+'/updateCascadingProject';
        var cascadingProject = jQuery(this).val();
        new Ajax.Request(jobUrl+'?projectName='+cascadingProject, {
            method : 'get',
            onSuccess : function(x) {
                location.reload(true);
            }
        });
   });
}

function onProjectPropertyChanged() {
    jQuery('input').change(function() {
        var ref = jQuery(this).attr('id');
        var cascadingProperty = '';
        if (ref != '') {
            cascadingProperty = jQuery(this).attr('name');
        } else {
            var childRef = jQuery(this).parents('tr').attr('nameref');
            cascadingProperty = jQuery('#'+childRef).attr('name');
        }
        if(cascadingProperty !== undefined){
            var jobUrl = getJobUrl()+'/modifyCascadingProperty?propertyName='+cascadingProperty;
            new Ajax.Request(jobUrl, {
                method : 'get'
            });
        }
    });
}

jQuery(document).ready(function(){
    onCascadingProjectUpdated();
    onProjectPropertyChanged();
});

