
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
    if(isRunAsTest) return;
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
    if(isRunAsTest) return;
    var modify = function() {
        var ref = jQuery(this).attr('id');
        var cascadingProperty = '';
        if (ref != '') {
            cascadingProperty = jQuery(this).attr('name');
        } else {
            var parent = jQuery(this).parents('tr');
            while (parent.attr("nameref") == undefined && parent.size() !== 0) {
                parent = jQuery(parent).parents('tr');
            }
            var childRef = parent.attr("nameref");
            cascadingProperty = jQuery('#'+childRef).attr('name');
        }
        if(cascadingProperty !== undefined) {
            var jobUrl = getJobUrl()+'/modifyCascadingProperty?propertyName='+cascadingProperty;
            new Ajax.Request(jobUrl, {
                method : 'get'
            });
        }
    };
    jQuery('form[name=config] input, form[name=config] .setting-input').live("change", modify);
    jQuery('form[name=config] button').live("click", modify);
}

jQuery(document).ready(function(){
    onCascadingProjectUpdated();
    onProjectPropertyChanged();
});
