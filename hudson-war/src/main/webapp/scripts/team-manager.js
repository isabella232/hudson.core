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

var images = [
    imageRoot + '/green-check.jpg',
    imageRoot + '/progressbar.gif',
    imageRoot + '/error.png',
    imageRoot + '/16x16/warning.png'
];

var pageInitialized = false;
var selectedTeam;

jQuery(document).ready(function() {

    //To avoid multiple fire of document.ready
    if (pageInitialized) {
        return;
    }
    pageInitialized = true;

    jQuery("#teamManagerTabs").tabs();
    jQuery("#teamAdminTabs").tabs();

    jQuery("#outerTabs tr").not(".header").hover(
            function() {
                jQuery(this).css("background", "#EEEDED");
            },
            function() {
                jQuery(this).css("background", "");
            }
    );

    if (jQuery("#selectableTeamList").length > 0) {
        jQuery("#selectableTeamList").selectable({
            selected: function(event, ui) {
                jQuery(ui.selected).siblings().removeClass("ui-selected");
                selectedTeam = jQuery(ui.selected);
                jQuery("#teamInfo").load('teams/' + jQuery(selectedTeam).text(), function() {
                    onTeamDetailsLoad();
                });
            }
        });

        // select the first team, so that its details can be loaded
        selectSelectableElement(jQuery("#selectableTeamList"), jQuery("#selectableTeamList").children(":eq(0)"));
    }

    var createTeamButton = jQuery('#createTeamButton');
    createTeamButton.button();
    createTeamButton.unbind("click").click(function() {
        createTeamButtonAction();
    });

    var moveJobsButton = jQuery('#moveJobsButton');
    moveJobsButton.button();
    moveJobsButton.unbind("click").click(function() {
        moveJobsButtonAction();
    });

    if (typeof currentUserTeam !== 'undefined') {
        jQuery("#teamInfo").load('teams/' + currentUserTeam, function() {
            onTeamDetailsLoad();
        });
    }
});

function onTeamDetailsLoad() {

    jQuery("#teamAdminTabs").tabs();

    jQuery('#teamInfo button.teamDeleteButton').each(function() {
        jQuery(this).button();
        jQuery(this).addClass('redButton');
        jQuery(this).unbind("click").click(function() {
            deleteTeamButtonAction(this);
        });
    });

    jQuery('#teamInfo button.teamMemberAddButton').each(function() {
        jQuery(this).button();
        jQuery(this).unbind("click").click(function() {
            teamMemberAddButtonAction(jQuery(this).val());
        });
    });

    jQuery('#teamInfo img.teamMemberUpdate').each(function() {
        jQuery(this).unbind("click").click(function() {
            updateTeamMemberAction(this);
        });
    });

    jQuery('#teamInfo img.teamMemberRemove').each(function() {
        jQuery(this).unbind("click").click(function() {
            removeTeamMemberAction(this);
        });
    });

    jQuery('#teamInfo img.configureJobVisibility').each(function() {
        jQuery(this).unbind("click").click(function() {
            configureJobVisibilityAction(this);
        });
    });

    jQuery('#teamMemberTab tr').each(function() {
        verifySid(this);
    });
}

function selectSelectableElement(selectableContainer, elementToSelect) {
    jQuery("li", selectableContainer).each(function() {
        if (this != elementToSelect[0]) {
            jQuery(this).removeClass("ui-selected").addClass("ui-unselecting");
        }
    });
    elementToSelect.addClass("ui-selecting");
    selectableContainer.data("selectable")._mouseStop(null);
}

function  createTeamButtonAction() {
    jQuery("#teamDesc").attr('value', ""); 
    showMessage("", false, jQuery('#teamAddMsg'));
    jQuery('#dialog-create-team').dialog({
        resizable: false,
        height: 250,
        width: 600,
        modal: true,
        buttons: {
            'Create': function() {
                var teamName = jQuery("#teamName").val();
                var teamDesc = jQuery("#teamDesc").val();
                var teamFolder = jQuery("#teamCustomFolder").val();
                if (!/^[a-zA-Z0-9]+$/.test(teamName)) {
                   showMessage("Only Alpha-Numeric characters allowed in team name.", true, jQuery('#teamAddMsg'));
                } else {
                    createTeam(jQuery.trim(teamName), jQuery.trim(teamDesc), jQuery.trim(teamFolder));
                }
            },
            Cancel: function() {
                jQuery(this).dialog("close");
            }
        }
    });
}

function createTeam(teamName, teamDesc, teamFolder) {
    jQuery.ajax({
        type: 'POST',
        url: "createTeam",
        data: {
            teamName: teamName,
            description: teamDesc,
            customFolder: teamFolder
        },
        success: function(result) {
            jQuery("#noTeamsMsg").hide();
            jQuery("#teamInfo").show();
            jQuery("#teamList").show();
            var teamItem = jQuery('<li class="ui-widget-content" title="' + teamName + '">' + teamName + '</li>');
            jQuery(teamItem).appendTo(jQuery('#selectableTeamList'));
            jQuery('#dialog-create-team').dialog("close");
            jQuery("#teamInfo").load('teams/' + teamName, function() {
                onTeamDetailsLoad();
            });
            selectSelectableElement(jQuery("#selectableTeamList"), jQuery("#selectableTeamList li:last-child"));
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
    jQuery.ajax({
        type: 'POST',
        url: "deleteTeam",
        data: {
            teamName: teamName,
        },
        success: function(result) {
            var nextSelectable = jQuery(selectedTeam).next();
            if (nextSelectable.length == 0) {
                nextSelectable = jQuery(selectedTeam).prev();
            }
            jQuery(selectedTeam).remove();
            if (nextSelectable.length > 0) {
                //selectSelectableElement(jQuery("#selectableTeamList"), jQuery("#selectableTeamList li:first-child"));
                selectSelectableElement(jQuery("#selectableTeamList"), jQuery(nextSelectable));
            } else {
                jQuery("#noTeamsMsg").show();
                jQuery("#teamInfo").hide();
                jQuery("#teamList").hide();
            }
            jQuery('#dialog-delete-team').dialog("close");
        },
        error: function(msg) {
            showMessage(msg.responseText, true, jQuery('#teamDeleteMsg'));
        }
    });
}

function teamMemberAddButtonAction(teamName) {
    jQuery("#cb_adminFlag").prop('checked', false);
    jQuery("#cb_createFlag").prop('checked', true);
    jQuery("#cb_deleteFlag").prop('checked', true);
    jQuery("#cb_configureFlag").prop('checked', true);
    jQuery("#cb_buildFlag").prop('checked', true);

    jQuery("#text_sidName").show();
    jQuery("#label_sidName").hide();
    jQuery('#dialog-add-modify-user').dialog({
        resizable: false,
        height: 300,
        width: 450,
        modal: true,
        title: "Add Team Member",
        buttons: {
            'Add': function() {
                var sid = jQuery("#text_sidName").val();
                var adminFlag = jQuery("#cb_adminFlag").is(':checked');
                var createFlag = jQuery("#cb_createFlag").is(':checked');
                var deleteFlag = jQuery("#cb_deleteFlag").is(':checked');
                var configureFlag = jQuery("#cb_configureFlag").val();
                var buildFlag = jQuery("#cb_buildFlag").val();
                addTeamMember(teamName, sid, adminFlag, createFlag, deleteFlag, configureFlag, buildFlag);
            },
            Cancel: function() {
                jQuery(this).dialog("close");
            }
        }
    });
}

function addTeamMember(teamName, member, adminFlag, createFlag, deleteFlag, configureFlag, buildFlag) {
    jQuery.ajax({
        type: 'POST',
        url: "addTeamMember",
        data: {
            teamName: teamName,
            teamMemberSid: member,
            isTeamAdmin: adminFlag,
            canCreate: createFlag,
            canDelete: deleteFlag,
            canConfigure: configureFlag,
            canBuild: buildFlag
        },
        success: function(iconNameResponse) {
            jQuery('#teamMemberNone_' + teamName).remove();
            jQuery('#teamMemberListHeader1_' + teamName).css('visibility', 'visible');
            jQuery('#teamMemberListHeader2_' + teamName).css('visibility', 'visible');

            var userTemplate = jQuery("#userTemplate tr").clone();
            jQuery("input[name='hiddenUserName']", userTemplate).attr("value", member);
            jQuery("input[name='hiddenTeamName']", userTemplate).attr("value", teamName);
            var icon = jQuery(userTemplate).find("img[name='typeIcon']");
            jQuery(icon).attr("src", imageRoot + "/16x16/" + iconNameResponse);
            jQuery("span", userTemplate).text(member);

            var adminIcon = jQuery(userTemplate).find("img[name='adminIcon']");
            adminIcon.css('visibility', adminFlag ? 'visible' : 'hidden');
            var createIcon = jQuery(userTemplate).find("img[name='createIcon']");
            createIcon.css('visibility', createFlag ? 'visible' : 'hidden');
            var deleteIcon = jQuery(userTemplate).find("img[name='deleteIcon']");
            deleteIcon.css('visibility', deleteFlag ? 'visible' : 'hidden');
            var configureIcon = jQuery(userTemplate).find("img[name='configureIcon']");
            configureIcon.css('visibility', configureFlag ? 'visible' : 'hidden');
            var buildIcon = jQuery(userTemplate).find("img[name='buildIcon']");
            buildIcon.css('visibility', buildFlag ? 'visible' : 'hidden');

            var updateIcon = jQuery(userTemplate).find("img[name='updateIcon']");
            jQuery(updateIcon).addClass("teamMemberUpdate");
            jQuery(updateIcon).unbind("click").click(function() {
                updateTeamMemberAction(this);
            });

            var removeIcon = jQuery(userTemplate).find("img[name='removeIcon']");
            jQuery(removeIcon).addClass("teamMemberRemove");
            jQuery(removeIcon).unbind("click").click(function() {
                removeTeamMemberAction(this);
            });

            var teamMemberList = jQuery('#teamMemberList_' + teamName);
            jQuery(userTemplate).appendTo(jQuery(teamMemberList));

            jQuery('#dialog-add-modify-user').dialog("close");
        },
        error: function(msg) {
            showMessage(msg.responseText, true, jQuery('#userAddMsg'));
        },
        dataType: "html"
    });
}

function updateTeamMemberAction(updateItem) {
    var trParent = jQuery(updateItem).parents("tr:first");
    var memberName = jQuery(trParent).find("input[name='hiddenUserName']").val();
    var teamName = jQuery(trParent).find("input[name='hiddenTeamName']").val();

    var adminIcon = jQuery(trParent).find("img[name='adminIcon']");
    var adminFlag = jQuery(adminIcon).css("visibility") == "visible";
    jQuery("#cb_adminFlag").prop('checked', adminFlag)

    var createIcon = jQuery(trParent).find("img[name='createIcon']");
    var createFlag = jQuery(createIcon).css("visibility") == "visible";
    jQuery("#cb_createFlag").prop('checked', createFlag);

    var deleteIcon = jQuery(trParent).find("img[name='deleteIcon']");
    var deleteFlag = jQuery(deleteIcon).css("visibility") == "visible";
    jQuery("#cb_deleteFlag").prop('checked', deleteFlag);

    var configureIcon = jQuery(trParent).find("img[name='configureIcon']");
    var configureFlag = jQuery(configureIcon).css("visibility") == "visible";
    jQuery("#cb_configureFlag").prop('checked', configureFlag);

    var buildIcon = jQuery(trParent).find("img[name='buildIcon']");
    var buildFlag = jQuery(buildIcon).css("visibility") == "visible";
    jQuery("#cb_buildFlag").prop('checked', buildFlag);

    jQuery("#text_sidName").hide();
    jQuery("#label_sidName").show();
    jQuery("#label_sidName").text(memberName);

    jQuery('#dialog-add-modify-user').dialog({
        resizable: false,
        height: 300,
        width: 350,
        modal: true,
        title: "Update Team Member",
        buttons: {
            'Update': function() {
                adminFlag = jQuery("#cb_adminFlag").is(':checked');
                createFlag = jQuery("#cb_createFlag").is(':checked');
                deleteFlag = jQuery("#cb_deleteFlag").is(':checked');
                configureFlag = jQuery("#cb_configureFlag").is(':checked');
                buildFlag = jQuery("#cb_buildFlag").is(':checked');
                updateTeamMember(trParent, teamName, memberName, adminFlag, createFlag, deleteFlag, configureFlag, buildFlag);
            },
            Cancel: function() {
                jQuery(this).dialog("close");
            }
        }
    });
}

function updateTeamMember(trParent, teamName, member, adminFlag, createFlag, deleteFlag, configureFlag, buildFlag) {
    jQuery.ajax({
        type: 'POST',
        url: "updateTeamMember",
        data: {
            teamName: teamName,
            teamMemberSid: member,
            isTeamAdmin: adminFlag,
            canCreate: createFlag,
            canDelete: deleteFlag,
            canConfigure: configureFlag,
            canBuild: buildFlag
        },
        success: function(iconNameResponse) {
            jQuery('#teamMemberNone_' + teamName).remove();

            var icon = jQuery(trParent).find("img[name='typeIcon']");
            jQuery(icon).attr("src", imageRoot + "/16x16/" + iconNameResponse);
            jQuery("span", trParent).text(member);

            var adminIcon = jQuery(trParent).find("img[name='adminIcon']");
            adminIcon.css('visibility', adminFlag ? 'visible' : 'hidden');
            var createIcon = jQuery(trParent).find("img[name='createIcon']");
            createIcon.css('visibility', createFlag ? 'visible' : 'hidden');
            var deleteIcon = jQuery(trParent).find("img[name='deleteIcon']");
            deleteIcon.css('visibility', deleteFlag ? 'visible' : 'hidden');
            var configureIcon = jQuery(trParent).find("img[name='configureIcon']");
            configureIcon.css('visibility', configureFlag ? 'visible' : 'hidden');
            var buildIcon = jQuery(trParent).find("img[name='buildIcon']");
            buildIcon.css('visibility', buildFlag ? 'visible' : 'hidden');

            jQuery('#dialog-add-modify-user').dialog("close");
        },
        error: function(msg) {
            showMessage(msg.responseText, true, jQuery('#userAddMsg'));
        },
        dataType: "html"
    });
}

function removeTeamMemberAction(deleteItem) {
    var trParent = jQuery(deleteItem).parents("tr:first");
    var memberName = jQuery(trParent).find("input[name='hiddenUserName']").val();
    var teamName = jQuery(trParent).find("input[name='hiddenTeamName']").val();
    jQuery('#dialog-remove-user').dialog({
        resizable: false,
        height: 165,
        width: 400,
        modal: true,
        title: "Remove Team Member - " + memberName,
        buttons: {
            'Remove': function() {
                removeTeamMember(teamName, memberName, trParent);
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
            if (jQuery('#teamMemberList_' + teamName).find('tr').length < 3) {
                jQuery('#teamMemberListHeader1_' + teamName).css('visibility', 'hidden');
                jQuery('#teamMemberListHeader2_' + teamName).css('visibility', 'hidden');
            }
        },
        error: function(msg) {
            showMessage(msg.responseText, true, jQuery('#userRemoveMsg'));
        },
        dataType: "html"
    });
}

function configureJobVisibilityAction(configureJobItem) {
    var trParent = jQuery(configureJobItem).parents("tr:first");
    var jobName = jQuery(trParent).find("input[name='hiddenJobId']").val();
    var teamName = jQuery(trParent).find("input[name='hiddenTeamName']").val();
    var teamNames = jQuery(trParent).find("input[name='hiddenVisibilities']").val();

    jQuery('#dialog-configure-visibility').dialog({
        resizable: false,
        height: 300,
        width: 350,
        modal: true,
        title: "Update Job Visibility - " + jobName,
        buttons: {
            'Update': function() {
                teamNames = "";
                jQuery('#configure-visibility-team-list input[@type=checkbox]:checked').each(function() {
                    teamNames += (jQuery(this).val() + ":");
                });
                if (jQuery('#publicVisibility').is(":checked")) {
                    teamNames += "public";
                }
                jQuery(trParent).find("input[name='hiddenVisibilities']").val(teamNames);
                configureJobVisibility(jobName, teamNames);
            },
            Cancel: function() {
                jQuery(this).dialog("close");
            }
        }
    });

    jQuery.getJSON('getAllTeamsJson', function(json) {
        jQuery('#configure-visibility-team-list').empty();
        var publicItem = jQuery('#publicVisibility');
        if (teamNames.indexOf("public") >= 0) {
            jQuery(publicItem).prop('checked', true);
        } else {
            jQuery(publicItem).prop('checked', false);
        }
        jQuery.each(json, function(key, val) {
            if (key !== "public") {
                var item = jQuery("#team-visibility-item-template div").clone();
                jQuery(item).find("label").text(key);
                var input = jQuery(item).find("input");
                jQuery(input).val(key);
                if (key === teamName) {
                    jQuery(input).prop('checked', true);
                    jQuery(input).prop('disabled', true);
                } else {
                    jQuery(input).prop('checked', (teamNames.indexOf(key) >= 0));
                }
                jQuery(item).appendTo(jQuery('#configure-visibility-team-list'));
            }
        });
    });
}

function configureJobVisibility(jobName, teamNames) {
    jQuery.ajax({
        type: 'POST',
        url: "setJobVisibility",
        data: {
            jobName: jobName,
            teamNames: teamNames
        },
        success: function() {
            jQuery('#dialog-configure-visibility').dialog("close");
        },
        error: function(msg) {
            showMessage(msg.responseText, true, jQuery('#configureVisibilityMsg'));
        },
        dataType: "html"
    });
}

var moveCount;
var jobsToMove;
function moveJobsButtonAction() {
    jQuery("#selectedJobs").empty();
    jQuery('#moveJobMsg').hide();
    jobsToMove = getJobsToMove();
    moveCount = jobsToMove.length;
    for (var i = 0; i < jobsToMove.length; i++) {
        var item = '<li value="' + jobsToMove[i] + '">' + jobsToMove[i] + ' <img style="display: none"/></li>';
        jQuery(item).appendTo(jQuery("#selectedJobs"));
    }

    jQuery('#dialog-move-jobs').dialog({
        resizable: false,
        height: 200 + moveCount * 25,
        width: 400,
        modal: true,
        title: "Move Jobs to another Team",
        buttons: {
            'Move': function() {
                if (moveCount > 0) {
                    var teamName = jQuery("#teamChoice option:selected").val();
                    for (var i = 0; i < jobsToMove.length; i++) {
                        var img = jQuery("#selectedJobs li[value='" + jobsToMove[i] + "']").children('img');
                        jQuery(img).attr('src', imageRoot + '/spinner.gif');
                        jQuery(img).show();
                        moveJobs(jobsToMove[i], teamName, img);
                    }
                }
            },
            Cancel: function() {
                jQuery(this).dialog("close");
            }
        }
    });

    jQuery.getJSON('getTeamsJson', function(json) {
        jQuery('#teamChoice').empty();
        jQuery.each(json, function(key, val) {
            var item = '<option value="' + key + '">' + val + '</option>';
            jQuery(item).appendTo(jQuery('#teamChoice'));
        });
    });

}

function getJobsToMove() {
    var jobs = [];
    jQuery('#teamJobsContainer input[@type=checkbox]:checked').each(function() {
        jobs.push(jQuery(this).val());
    });
    return jobs;
}

function moveJobs(jobName, teamName, img) {
    jQuery('#teamJobsContainer input[@type=checkbox]:checked').each(function() {
        jQuery(this).prop('checked', false);
    });
    jQuery.ajax({
        type: 'POST',
        url: "moveJob",
        data: {
            jobName: jobName,
            teamName: teamName
        },
        success: function() {
            jQuery(img).attr('src', imageRoot + '/green-check.jpg');
            jQuery("#job_colum3_span_" + jobName.replace(".","\\.")).text(teamName);
            moveCount--;
            if (moveCount === 0) {
                jQuery('#dialog-move-jobs').dialog("close");
            }
        },
        error: function(msg) {
            var originalHeight = jQuery('#dialog-move-jobs').height();
            jQuery('#dialog-move-jobs').css({
                height: originalHeight + 50
            });
            jQuery(img).attr('src', imageRoot + '/16x16/error.png');
            showMessage(msg.responseText, true, jQuery('#moveJobMsg'));
        },
        dataType: "html"
    });
}

function verifySid(sidElement) {
    var sid = jQuery(sidElement).find("input[name='hiddenUserName']").val();
    if (sid) {
        jQuery.ajax({
            type: 'POST',
            url: "checkSid",
            data: {
                sid: sid
            },
            success: function(iconNameResponse) {
                var icon = jQuery(sidElement).find("img[name='typeIcon']");
                jQuery(icon).attr("src", imageRoot + "/16x16/" + iconNameResponse);
                jQuery(icon).css('visibility', 'visible');
            },
            dataType: "html"
        });
    }
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