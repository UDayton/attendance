/*
 *  Copyright (c) 2017, University of Dayton
 *
 *  Licensed under the Educational Community License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *              http://opensource.org/licenses/ecl2
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.sakaiproject.attendance.tool.pages;

import org.sakaiproject.attendance.tool.dataproviders.AttendanceStatusProvider;
import org.sakaiproject.attendance.model.*;

import java.util.*;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;

import org.apache.wicket.model.ResourceModel;



/**
 * Created by james on 6/8/17.
 */
public class ImportConfirmation  extends BasePage{
    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        disableLink(exportLink);


        add(new ImportConfirmation.UploadForm("form"));
    }
    private static final long serialVersionUID = 1L;
    private AttendanceStatusProvider attendanceStatusProvider;
    private DropDownChoice<String> groupChoice;
    private String selectedGroup;
    private List<ImportConfirmList> ICLpart2;

    public ImportConfirmation(List<ImportConfirmList> ICL, Boolean commentsChanged) {


        if(commentsChanged){
            System.out.println("Comments");

        } else {
            System.out.println("nope");
        }
        this.ICLpart2 = ICL;
        homepageLink = new Link<Void>("homepage-link2") {
            private static final long serialVersionUID = 1L;
            public void onClick() {

                setResponsePage(new Overview());
            }
        };
        homepageLink.add(new Label("homepage-link-label",new ResourceModel("attendance.link.homepage")).setRenderBodyOnly(true));
        homepageLink.add(new AttributeModifier("title", new ResourceModel("attendance.link.homepage.tooltip")));
        add(homepageLink);

        if(this.role != null && this.role.equals("Student")) {
            throw new RestartResponseException(StudentView.class);
        }

        this.attendanceStatusProvider = new AttendanceStatusProvider(attendanceLogic.getCurrentAttendanceSite(), AttendanceStatusProvider.ACTIVE);

        add(createStatsTable(ICL, commentsChanged));
    }

    private WebMarkupContainer createStatsTable(List<ImportConfirmList> ICL, boolean commentsChanged) {
        WebMarkupContainer  statsTable      = new WebMarkupContainer("student-overview-stats-table");
        createStatsTableHeader(statsTable, commentsChanged);
        createStatsTableData(statsTable, ICL, commentsChanged);
        return statsTable;
    }

    private void createStatsTableHeader(WebMarkupContainer t, boolean commentsChanged) {
        WebMarkupContainer oldCommentwmc1 = new WebMarkupContainer("old-comment-header");
        WebMarkupContainer newCommentwmc1 = new WebMarkupContainer("new-comment-header");
        t.add(oldCommentwmc1);
        t.add(newCommentwmc1);
        oldCommentwmc1.add(new Label("old-comment", "Old Comment"));
        if(commentsChanged){
            oldCommentwmc1.setVisible(true);
        } else {
            oldCommentwmc1.setVisible(false);
        }
        newCommentwmc1.add(new Label("new-comment", "New Comment"));
        if(commentsChanged){
            newCommentwmc1.setVisible(true);
        } else {
            newCommentwmc1.setVisible(false);
        }
    }

    private void createStatsTableData(WebMarkupContainer t, List<ImportConfirmList> ICL, boolean commentsChanged) {
        final Map<String, AttendanceGrade> gradeMap = attendanceLogic.getAttendanceGrades();

        final ListView<ImportConfirmList> uListView = new ListView<ImportConfirmList>("students", ICL) {
            int counter = 0;
            int counter2 = 0;
            int statusSwitch = 0;
            @Override
            protected void populateItem(ListItem<ImportConfirmList> item) {
                WebMarkupContainer newCommentwmc = new WebMarkupContainer("new-comment-wmc");
                WebMarkupContainer oldCommentwmc = new WebMarkupContainer("old-comment-wmc");
                String stat = "";
                if(ICL.get(counter).getEventDate().equals("NODATE")){
                    item.add(new Label("event-name-label", String.valueOf(ICL.get(counter).getEventName())));
                } else {
                    item.add(new Label("event-name-label", String.valueOf(ICL.get(counter).getEventName()) + "[" + String.valueOf(ICL.get(counter).getEventDate()) + "]"));
                }
                item.add(new Label("student-name-label", sakaiProxy.getUserSortName(ICL.get(counter).getUserID())));

                stat =String.valueOf(ICL.get(counter).getOldStatus());
                stat = changeStatString(stat);
                item.add(new Label("old-status-label", stat));

                stat = String.valueOf(ICL.get(counter).getOldComment());
                stat = changeStatString(stat);
                item.add(oldCommentwmc);
                oldCommentwmc.add(new Label("old-comment-label", stat));
                if(commentsChanged){
                    oldCommentwmc.setVisible(true);
                } else {
                    oldCommentwmc.setVisible(false);
                }

                stat = String.valueOf(ICL.get(counter).getStatus());
                stat = changeStatString(stat);
                item.add(new Label("new-status-label", stat));


                stat = String.valueOf(ICL.get(counter).getComment());
                stat = changeStatString(stat);
                item.add(newCommentwmc);
                newCommentwmc.add(new Label("new-comment-label", stat));
                if(commentsChanged){
                    newCommentwmc.setVisible(true);
                } else {
                    newCommentwmc.setVisible(false);
                }





                /*DataView<AttendanceStatus> activeStatusStats = new DataView<AttendanceStatus>("active-status-stats",  attendanceStatusProvider) {
                    @Override
                    protected void populateItem(Item<AttendanceStatus> statusItem) {
                        String stat = "";
                        if(statusSwitch == 0){
                            stat = sakaiProxy.getUserSortName(ICL.get(counter2).getUserID());
                            stat = changeStatString(stat);
                            statusSwitch = 1;
                        } else if (statusSwitch == 1){
                            stat = String.valueOf(ICL.get(counter2).getStatus());
                            stat = changeStatString(stat);
                            statusSwitch = 2;
                        } else if (statusSwitch == 2){
                            stat = String.valueOf(ICL.get(counter2).getComment());
                            stat = changeStatString(stat);
                            statusSwitch = 3;
                        } else if (statusSwitch == 3){
                            stat = String.valueOf(ICL.get(counter2).getOldStatus());
                            stat = changeStatString(stat);
                            statusSwitch = 4;
                        }else {
                            stat = String.valueOf(ICL.get(counter2).getOldComment());
                            stat = changeStatString(stat);
                            statusSwitch =0;
                            counter2++;
                        }
                        statusItem.add(new Label("student-stats", stat));
                    }
                };*/
                counter++;
                //item.add(activeStatusStats);
            }
        };

        Label noStudents = new Label("no-students", new ResourceModel("attendance.student.overview.no.students")) {
            @Override
            public boolean isVisible(){
                return uListView.size() <= 0;
            }
        };
        Label noStudents2 = new Label("no-students2", new ResourceModel("attendance.student.overview.no.students.2")) {
            @Override
            public boolean isVisible(){
                return uListView.size() <= 0;
            }
        };

        t.add(uListView);
        t.add(noStudents);
        t.add(noStudents2);
    }

    private String changeStatString(String stat){
        if(stat.equals("PRESENT")){
            stat = "Present";
        } else if(stat.equals("UNEXCUSED_ABSENCE")){
            stat = "Unexcused Absence";
        } else if(stat.equals("EXCUSED_ABSENCE")){
            stat = "Excused Absence";
        } else if(stat.equals("LATE")){
            stat = "Late";
        } else if(stat.equals("LEFT_EARLY")){
            stat = "Left Early";
        } else if(stat.equals("UNKNOWN")){
            stat = "";
        } else if(stat.equals("null")){
            stat = "";
        }
        return stat;
    }

    private class UploadForm extends Form<Void> {

        public UploadForm(final String id) {
            super(id);

            add(new SubmitLink("submitLink") {
                public void onSubmit() {
                    for (int i = 0; i < ICLpart2.size(); i++){
                        boolean updated = attendanceLogic.updateAttendanceRecord(ICLpart2.get(i).getAttendanceRecord(), ICLpart2.get(i).getOldStatus());
                        attendanceLogic.updateAttendanceSite(ICLpart2.get(i).getAttendanceSite());
                    }
                    getSession().success(getString("attendance.export.confirmation.import.save.success"));
                    setResponsePage(new Overview());
                }
            });
            add(new SubmitLink("submitLink2") {
                public void onSubmit() {
                    setResponsePage(new ExportPage());
                }
            });
        }
    }

}
