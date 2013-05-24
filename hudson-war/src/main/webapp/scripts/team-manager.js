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

var pageInitialized = false;

jQuery(document).ready(function() {

    //To avoid multiple fire of document.ready
    if (pageInitialized){
        return;
    }
    pageInitialized = true;

    var createTeamButton = jQuery('#createTeamButton');
    createTeamButton.button();
    createTeamButton.unbind("click").click(function() {
        createTeamButtonAction();
    });

    jQuery('#teamContainer button.teamDeleteButton').each(function() {
        jQuery(this).button();
        jQuery(this).addClass('redButton');
        jQuery(this).unbind("click").click(function() {
            deleteTeamButtonAction(this);
        });
    });

    jQuery('#teamContainer button.teamAdminAddButton').each(function() {
        jQuery(this).button();
        jQuery(this).unbind("click").click(function() {
            teamAdminAddButtonAction(jQuery(this).val());
        });
    });

    jQuery('#teamContainer img.teamAdminRemove').each(function() {
        jQuery(this).unbind("click").click(function() {
            removeTeamAdminAction(this);
        });
    });

    jQuery('#teamContainer button.teamMemberAddButton').each(function() {
        jQuery(this).button();
        jQuery(this).unbind("click").click(function() {
            teamMemberAddButtonAction(jQuery(this).val());
        });
    });

    jQuery('#teamContainer img.teamMemberRemove').each(function() {
        jQuery(this).unbind("click").click(function() {
            removeTeamMemberAction(this);
        });
    });


    jQuery('.teamList li').each(function() {
        verifySid(this);
    });

});

function  createTeamButtonAction() {
    jQuery('#dialog-create-team').dialog({
        resizable: false,
        height: 200,
        width: 450,
        modal: true,
        buttons: {
            'Create': function() {
                var teamName = jQuery("#teamName").val();
                var teamDesc = jQuery("#teamDesc").val();
                createTeam(teamName, teamDesc);
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
            teamName: teamName,
            description: teamDesc
        },
        success: function(result) {
            var resultItem = jQuery(result);
            jQuery(resultItem).appendTo(jQuery('ul.team'));
            var teamAdminAddButton = jQuery('.teamAdminAddButton', jQuery(resultItem));
            teamAdminAddButton.button();
            teamAdminAddButton.unbind("click").click(function() {
                teamAdminAddButtonAction(jQuery(this).val());
            });
            var teamMemberAddButton = jQuery('.teamMemberAddButton', jQuery(resultItem));
            teamMemberAddButton.button();
            teamMemberAddButton.unbind("click").click(function() {
                teamMemberAddButtonAction(jQuery(this).val())
            });
            var teamDeleteButton = jQuery('.teamDeleteButton', jQuery(resultItem));
            teamDeleteButton.button();
            teamDeleteButton.unbind("click").click(function() {
                deleteTeamButtonAction(this);
            });
            jQuery('#dialog-create-team').dialog("close");
        },
        error: function(msg) {
            showMessage(msg.responseText, true, jQuery('#teamAddMsg'));
        }
    });
}

function  deleteTeamButtonAction(deleteButton) {
    var teamName = jQuery(deleteButton).val();
    jQuery('#dialog-delete-team').dialog({
        resizable: false,
        height: 150,
        width: 450,
        modal: true,
        title: "Delete Team - " + teamName,
        buttons: {
            'Delete': function() {
                deleteTeam(deleteButton);
            },
            Cancel: function() {
                jQuery(this).dialog("close");
            }
        }
    });
}

function deleteTeam(deleteButton) {
    var teamName = jQuery(deleteButton).val();
    var parent = jQuery(deleteButton).parent();
    jQuery.ajax({
        type: 'POST',
        url: "deleteTeam",
        data: {
            teamName: teamName,
        },
        success: function(result) {
            jQuery(parent).remove();
            jQuery('#dialog-delete-team').dialog("close");
        },
        error: function(msg) {
            showMessage(msg.responseText, true, jQuery('#teamDeleteMsg'));
        }
    });
}

function teamAdminAddButtonAction(teamName) {
    jQuery('#dialog-add-user').dialog({
        resizable: false,
        height: 165,
        width: 400,
        modal: true,
        title: "Add Team Admin",
        buttons: {
            'Add': function() {
                var sid = jQuery("#sidName").val();
                addTeamAdmin(teamName, sid);
            },
            Cancel: function() {
                jQuery(this).dialog("close");
            }
        }
    });
}

function addTeamAdmin(teamName, adminSid) {
    jQuery.ajax({
        type: 'POST',
        url: "addTeamAdmin",
        data: {
            teamName: teamName,
            teamAdminSid: adminSid
        },
        success: function(iconNameResponse) {
            jQuery('#teamAdminNone_' + teamName).remove();

            var userTemplate = jQuery("#userTemplate li").clone();
            jQuery("input[name='hiddenUserName']", userTemplate).attr("value", adminSid);
            jQuery("input[name='hiddenTeamName']", userTemplate).attr("value", teamName);
            var icon = jQuery(userTemplate).children("img[name='typeIcon']");
            jQuery(icon).attr("src", imageRoot + "/16x16/" + iconNameResponse);
            jQuery("span", userTemplate).text(adminSid);
            var deleteIcon = jQuery(userTemplate).children("img[name='deleteIcon']");
            jQuery(deleteIcon).addClass("teamAdminRemove");
            jQuery(deleteIcon).unbind("click").click(function() {
                removeTeamAdminAction(this);
            });
            jQuery(userTemplate).appendTo(jQuery('#teamAdminList_' + teamName));

            jQuery('#dialog-add-user').dialog("close");
        },
        error: function(msg) {
            showMessage(msg.responseText, true, jQuery('#userAddMsg'));
        },
        dataType: "html"
    });
}

function removeTeamAdminAction(deleteItem) {
    var adminName = jQuery(deleteItem).siblings("input[name='hiddenUserName']").val();
    var teamName = jQuery(deleteItem).siblings("input[name='hiddenTeamName']").val();
    var parent = jQuery(deleteItem).parent();
    jQuery('#dialog-remove-user').dialog({
        resizable: false,
        height: 165,
        width: 400,
        modal: true,
        title: "Remove Team Admin - " + adminName,
        buttons: {
            'Remove': function() {
                removeTeamAdmin(teamName, adminName, parent);
            },
            Cancel: function() {
                jQuery(this).dialog("close");
            }
        }
    });
}

function removeTeamAdmin(teamName, adminName, parent) {
    jQuery.ajax({
        type: 'POST',
        url: "removeTeamAdmin",
        data: {
            teamName: teamName,
            teamAdminSid: adminName
        },
        success: function(msg) {
            parent.remove();
            jQuery('#dialog-remove-user').dialog("close");
        },
        error: function(msg) {
            showMessage(msg.responseText, true, jQuery('#userRemoveMsg'));
        },
        dataType: "html"
    });
}

function teamMemberAddButtonAction(teamName) {
    jQuery('#dialog-add-user').dialog({
        resizable: false,
        height: 165,
        width: 400,
        modal: true,
        title: "Add Team Member",
        buttons: {
            'Add': function() {
                var sid = jQuery("#sidName").val();
                addTeamMember(teamName, sid);
            },
            Cancel: function() {
                jQuery(this).dialog("close");
            }
        }
    });
}

function addTeamMember(teamName, member) {
    jQuery.ajax({
        type: 'POST',
        url: "addTeamMember",
        data: {
            teamName: teamName,
            teamMemberSid: member
        },
        success: function(iconNameResponse) {
            jQuery('#teamMemberNone_' + teamName).remove();

            var userTemplate = jQuery("#userTemplate li").clone();
            jQuery("input[name='hiddenUserName']", userTemplate).attr("value", member);
            jQuery("input[name='hiddenTeamName']", userTemplate).attr("value", teamName);
            var icon = jQuery(userTemplate).children("img[name='typeIcon']");
            jQuery(icon).attr("src", imageRoot + "/16x16/" + iconNameResponse);
            jQuery("span", userTemplate).text(member);
            var deleteIcon = jQuery(userTemplate).children("img[name='deleteIcon']");
            jQuery(deleteIcon).addClass("teamMemberRemove");
            jQuery(deleteIcon).unbind("click").click(function() {
                removeTeamMemberAction(this);
            });
            jQuery(userTemplate).appendTo(jQuery('#teamMemberList_' + teamName));

            jQuery('#dialog-add-user').dialog("close");
        },
        error: function(msg) {
            showMessage(msg.responseText, true, jQuery('#userAddMsg'));
        },
        dataType: "html"
    });
}

function removeTeamMemberAction(deleteItem) {
    var memberName = jQuery(deleteItem).siblings("input[name='hiddenUserName']").val();
    var teamName = jQuery(deleteItem).siblings("input[name='hiddenTeamName']").val();
    var parent = jQuery(deleteItem).parent();
    jQuery('#dialog-remove-user').dialog({
        resizable: false,
        height: 165,
        width: 400,
        modal: true,
        title: "Remove Team Member - " + memberName,
        buttons: {
            'Remove': function() {
                removeTeamMember(teamName, memberName, parent);
            },
            Cancel: function() {
                jQuery(this).dialog("close");
            }
        }
    });
}

function removeTeamMember(teamName, memberName, parent) {
    jQuery.ajax({
        type: 'POST',
        url: "removeTeamMember",
        data: {
            teamName: teamName,
            teamMemberSid: memberName
        },
        success: function() {
            parent.remove();
            jQuery('#dialog-remove-user').dialog("close");
        },
        error: function(msg) {
            showMessage(msg.responseText, true, jQuery('#userRemoveMsg'));
        },
        dataType: "html"
    });
}

function verifySid(sidElement) {
    var sid = jQuery(sidElement).children('span').text();
    jQuery.ajax({
        type: 'POST',
        url: "checkSid",
        data: {
            sid: sid
        },
        success: function(iconNameResponse) {
            var icon = jQuery(sidElement).children("img[name='typeIcon']");
            jQuery(icon).attr("src", imageRoot + "/16x16/" + iconNameResponse);
            jQuery(icon).css('visibility', 'visible');
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