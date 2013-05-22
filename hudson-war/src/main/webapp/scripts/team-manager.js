/*  
 * ******************************************************************************
 *  Copyright (c) 2013 Oracle Corporation.
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

var filesToUpload;
var updateCount;
var installCount;

jQuery(document).ready(function() {

    var createTeamButton = jQuery('#createTeamButton');
    createTeamButton.button();
    createTeamButton.unbind("click").click(function() {
        createTeamButtonAction();
    });

    jQuery('#teamContainer button.teamAdminAddButton').each(function() {
        jQuery(this).button();
        jQuery(this).unbind("click").click(function() {
            teamAdminAddButtonAction(this);
        });
    });
    
    jQuery('#teamContainer button.teamMemberAddButton').each(function() {
        jQuery(this).button();
        jQuery(this).unbind("click").click(function() {
            teamMemberAddButtonAction(this)
        });
    });

    jQuery('.teamList li').each(function() {
            verifySid(this);
    });
            
});

function  createTeamButtonAction() {
    jQuery('#dialog-create-team').dialog({
        resizable: false,
        height: 185,
        width: 450,
        modal: true,
        buttons: {
            'create': function() {
                var teamName = jQuery("#teamName").val();
                var teamDesc = jQuery("#teamDesc").val();
                createTeam(teamName, teamDesc);
                jQuery(this).dialog("close");
            },
            Cancel: function() {
                jQuery(this).dialog("close");
            }
        }
    });
}

function createTeam(teamName, teamDesc) {

    jQuery.ajax({
        type: 'POST',
        url: "createTeam",
        data: {
            name: teamName,
            description: teamDesc
        },
        success: function(result) {
            var resultItem = jQuery(result);
            jQuery(resultItem).appendTo(jQuery('ul.team'));
            jQuery('.teamAdminAddButton', resultItem).click(teamAdminAddButtonAction(this));
            jQuery('.teamMemberAddButton', resultItem).click(teamMemberAddButtonAction(this));
            showMessage("Team " + teamName + " created.", true, jQuery('#createTeamMsg'));
        },
        error: function(msg) {
            showMessage(msg.responseText, true, jQuery('#createTeamMsg'));
        }
    });
}

function teamAdminAddButtonAction(admin) {
    var teamName = jQuery(admin).val();
    var adminSid = jQuery("#teamAdminSid_" + teamName).val();
    jQuery.ajax({
        type: 'POST',
        url: "addTeamAdmin",
        data: {
            teamName: teamName,
            teamAdminSid: adminSid
        },
        success: function(msg) {
            jQuery('#teamAdminNone_' + teamName).remove();
            var teamAdminItem = '<li>' + msg + '</li>';
            jQuery(teamAdminItem).appendTo(jQuery('#teamAdminList_' + teamName));
        },
        error: function(msg) {
            showMessage(msg.responseText, true, jQuery('#teamAdminAddMsg_' + teamName));

        },
        dataType: "html"
    });
}

function teamMemberAddButtonAction(member) {
    var teamName = jQuery(member).val();
    var memberSid = jQuery("#teamMemberSid_" + teamName).val();
    jQuery.ajax({
        type: 'POST',
        url: "addTeamMember",
        data: {
            teamName: teamName,
            teamMemberSid: memberSid
        },
        success: function(msg) {
            jQuery('#teamMemberNone_' + teamName).remove();
            var sysAdminItem = '<li>' + msg + '</li>';
            jQuery(sysAdminItem).appendTo(jQuery('#teamMemberList_' + teamName));
        },
        error: function(msg) {
            showMessage(msg.responseText, true, jQuery('#teamMemberAddMsg_' + teamName));

        },
        dataType: "html"
    });
}

function verifySid(sidElement) {
    var sid = jQuery(sidElement).text();
    var parent = jQuery(sidElement).parent();
    jQuery(sidElement).remove();
    jQuery.ajax({
        type: 'POST',
        url: "checkSid",
        data: {
            sid: sid
        },
        success: function(msg) {
            var sidItem = '<li>' + msg + '</li>';
            jQuery(sidItem).appendTo(jQuery(parent));
        },
        error: function(msg) {
            var errorItem = '<li>' + msg + '</li>';
            jQuery(errorItem).appendTo(jQuery(parent));

        },
        dataType: "html"
    });
}

function showMessage(msg, error, infoTxt) {
    infoTxt.text(msg);
    if (error) {
        infoTxt.css("color", "red");
    } else {
        infoTxt.css("color", "green");
    }
    infoTxt.show();
}