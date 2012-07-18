/*  
 * ******************************************************************************
 *  Copyright (c) 2012 Oracle Corporation.
 * 
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors: 
 * 
 *     Winston Prakash
 *    
 * ******************************************************************************  
 */

jQuery.noConflict();
        
var loggedIn = false;
        
var installableCount = 0;
var installedCount = 0;
        
var finish = false;
var forProxy = false;
  
var canContinue = false;
        
function installPlugin(selected){
    jQuery('#errorMessage').hide();
    jQuery(selected).hide();
    var icon = jQuery("#" + jQuery(selected).val());
    jQuery(icon).show();
    icon.attr('src',imageRoot + '/progressbar.gif');
    jQuery.ajax({
        type: 'POST',
        url: "installPlugin",
        data: {
            pluginName:jQuery(selected).val()
        },
        success: function(){
            icon.attr('src',imageRoot + '/green-check.jpg');
            jQuery(selected).attr("checked", false);
            installedCount++;
            if (installedCount == installableCount){
                enableButtons();
                if (finish == true){
                    checkFinish();
                }
            }
        },
        error: function(){
            icon.attr('src',imageRoot + '/error.png');
            installedCount++;
            if (installedCount == installableCount){
                enableButtons();
            }
        },
        statusCode: {
            403: function() {
                showLoginDialog();
            }
        },
        dataType: "html"
    }); 
}
        
        
function disableButtons(){
    jQuery('#installButton').attr("disabled", true);
    jQuery('#finishButton').attr("disabled", true);
}
        
function enableButtons(){
    var installables = getInstallables();
    if (installables.length > 0){
        jQuery('#installButton').removeAttr("disabled");
    }
    jQuery('#finishButton').removeAttr("disabled");
           
    if (jQuery('#mandatoryPlugins input[@type=checkbox]:checked').length > 0){
        jQuery('#mandatoryMsg').show();
        jQuery('#recommendedMsg').hide();
    }else{
        jQuery('#mandatoryMsg').hide();
        jQuery('#recommendedMsg').show();
    }
}
        
function checkPermissionAndinstallPlugins(){
    var installables = getInstallables();
    if (installables.length == 0){
        if (finish == true){
            needsAdminLogin = false; 
        }
    }
    if (needsAdminLogin == true) {
        if (loggedIn == false){
            showLoginDialog();
        }else{
            installSelectedPlugins();
        }
    }else{
        installSelectedPlugins();
    }
}
        
function installSelectedPlugins(){
    var installables = getInstallables();
    installableCount = installables.length;
    if (installableCount == 0){
        checkFinish();
        return;
    }
    disableButtons();
    jQuery(installables).each(function(){
        installPlugin(this);
    });
}
        
function getInstallables(){
    var installables = [];
    jQuery('#mandatoryPlugins input[@type=checkbox]:checked').each(function(){
        installables.push(this);
    });
    jQuery('#featuredPlugins input[@type=checkbox]:checked').each(function(){
        installables.push(this);
    });
    jQuery('#recommendedPlugins input[@type=checkbox]:checked').each(function(){
        installables.push(this);
    });
    return installables;
}
        
function showLoginDialog(){
    jQuery.blockUI({
        message: jQuery('#loginDialog'),
        css: { 
            width: '350px'
        },
        title:  'Confirmation'
    });
    jQuery('j_username').focus();
}

function submitPoxyForm(){
    forProxy = false;
    jQuery('#proxySuccess').hide();
    jQuery('#proxyError').hide();
    var dataString = jQuery("#proxyForm").serialize();
    jQuery.ajax({
        type: 'POST',
        url: "proxyConfigure",
        data: dataString,
        success: function(){
            jQuery('#proxySuccess').show();
            enableButtons();
        },
        error: function(){
            jQuery('#proxyError').show();
        },
        statusCode: {
            403: function() {
                forProxy = true;
                showLoginDialog();
            }
        },
        dataType: "html"
    }); 
}
        
function submitLoginForm(){
    jQuery('#loginMsg').show();
    jQuery('#loginError').hide();
    var dataString = jQuery("#loginForm").serialize();
    jQuery.ajax({
        type: 'POST',
        url: loginUrl,
        data: dataString,
        success: function(){
            jQuery.unblockUI();
            loggedIn = true;
            if (forProxy == true){
                submitPoxyForm();
            }else{
                checkPermissionAndinstallPlugins();
            }
            jQuery('#loginNeededMsg').hide();
        },
        error: function(msg){
            jQuery('#loginError').text(msg.responseText);
            jQuery('#loginError').show();
            jQuery('#loginMsg').hide();
        },
        dataType: "html"
    }); 
}
        
function checkFinish(){
             
    jQuery.ajax({
        type: 'GET',
        url: "checkFinish",
        success: function(){
            window.location.href=".";
        },
        error: function(){
            jQuery('#instalMandatoryMsg').show();
        },
        dataType: "html"
    }); 
}
        
function refreshProxyUser(){
    if (jQuery('#proxyAuth').is(':checked')){
        jQuery('#proxyUser').show();
        jQuery('#proxyPassword').show();
    }else{
        jQuery('#proxyUser').hide();
        jQuery('#proxyPassword').hide();
    }
}

function doContinue(url){
    jQuery.ajax({
        type: 'GET',
        url: url,
        success: function(){
            window.location.href=".";
        },
        dataType: "html"
    }); 
}

jQuery(document).ready(function() {
        
    var images = [
    imageRoot + '/green-check.jpg',
    imageRoot + '/progressbar.gif',
    imageRoot + '/error.png'
    ];

    jQuery(images).each(function() {
        jQuery('<img />').attr('src', this);
    });
        
    jQuery('#j_username').keypress(function(e){
        if(e.which == 13){
            submitLoginForm();
        }
    });
            
    jQuery('#j_password').keypress(function(e){
        if(e.which == 13){
            submitLoginForm();
        }
    });

    jQuery('#loginButton').button();
    jQuery('#loginButton').click(function() {
        submitLoginForm();
    });
            
    jQuery('#cancelButton').button();
    jQuery('#cancelButton').click(function() {
        jQuery.unblockUI();
        jQuery('#j_username').attr({
            value:""
        });
        jQuery('#j_password').attr({
            value:""
        });
        jQuery('#loginError').hide();
        jQuery('#loginMsg').hide();
        return false;
    });
        
    jQuery('#installButton').button();   
    jQuery('#installButton').click(function() {
        installableCount = 0;
        installedCount = 0;
        checkPermissionAndinstallPlugins();
    });
            

    jQuery('#finishButton').button();
    jQuery('#finishButton').click(function() {
        installableCount = 0;
        installedCount = 0;
        checkPermissionAndinstallPlugins();
        finish = true;
    });
            
    jQuery('#proxyAuth').click(function() {
        refreshProxyUser();
    });
            
    if (proxyNeeded == true){
        jQuery('#proxyButton').click(function() {
            submitPoxyForm();
        });
    }else{
        jQuery('#proxySetup').hide();
    }
    
    jQuery('#continueButton').button();
    jQuery('#continueButton').click(function() {
        canContinue = true;
        if (securitySet == true) {
            if (loggedIn == false){
                showLoginDialog();
            } else {
                doContinue("continue");
            }
        }
    });
    
    jQuery('#fpFinishButton').button();
    jQuery('#fpFinishButton').click(function() {
        doContinue("finish")
    });
            
    if (canFinish == true){
        jQuery('#fpFinishButton').show();
        jQuery('#fpRecommendedMsg').show();
        jQuery('#fpMandatoryMsg').hide();
    }else{
        jQuery('#fpMandatoryMsg').show();
        jQuery('#fpFinishButton').hide();
    }
            
    refreshProxyUser();
            
    enableButtons();

});