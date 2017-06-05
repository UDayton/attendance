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

import org.sakaiproject.attendance.model.*;
import org.sakaiproject.user.api.User;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFCell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.*;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Created by james on 5/18/17.
 */
public class ExportPage extends BasePage{
    enum ExportFormat {
        XLS
    }
    private String holder = "";
    private int rowCounter = 0;
    private static final long serialVersionUID = 1L;
    public ExportPage() {
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        disableLink(exportLink);
        Model<AttendanceSite> siteModel = new Model<>(attendanceLogic.getCurrentAttendanceSite());
        Form<AttendanceSite> exportForm = new Form<>("export-form", siteModel);
        add(exportForm);
        exportForm.add(new DownloadLink("submit-link", new LoadableDetachableModel<File>() {
            private static final long serialVersionUID = 1L;
            @Override
            protected File load() {
                return buildExcelFile(false, true);
            }
        }).setCacheDuration(Duration.NONE).setDeleteAfterDownload(true));

        Form<AttendanceSite> exportForm2 = new Form<>("export-form2", siteModel);
        add(exportForm2);
        exportForm2.add(new DownloadLink("submit-link2", new LoadableDetachableModel<File>() {
            private static final long serialVersionUID = 1L;
            @Override
            protected File load() {
                return buildExcelFile(false, false);
            }
        }).setCacheDuration(Duration.NONE).setDeleteAfterDownload(true));

        Form<AttendanceSite> blankExportForm = new Form<>("export-formblank", siteModel);
        add(blankExportForm);
        blankExportForm.add(new DownloadLink("submit-link2", new LoadableDetachableModel<File>() {
            private static final long serialVersionUID = 1L;
            @Override
            protected File load() {
                return buildExcelFile(true ,true);
            }
        }).setCacheDuration(Duration.NONE).setDeleteAfterDownload(true));
        add(new UploadForm("form"));
    }

    private File buildExcelFile(boolean blankSheet, boolean commentsOnOff) {
        File tempFile;
        try {
            tempFile = File.createTempFile(buildFileNamePrefix(), buildFileNameSuffix());
            final HSSFWorkbook wb = new HSSFWorkbook();
            int eventCount;
            int studentCount;
            int columnFinder = 0;

            // Create new sheet
            HSSFSheet mainSheet = wb.createSheet("Export");
            // Create Excel header
            final List<String> header = new ArrayList<String>();
            final String selectedGroup = null;
            final List<AttendanceEvent> eventHolder = new ArrayList<AttendanceEvent>();
            AttendanceSite attendanceSite = attendanceLogic.getAttendanceSite(sakaiProxy.getCurrentSiteId());
            List<AttendanceEvent> attendanceEventlist = attendanceLogic.getAttendanceEventsForSite(attendanceSite);
            List<AttendanceUserStats> userStatsList = attendanceLogic.getUserStatsForCurrentSite(selectedGroup);
            Collections.sort(userStatsList, new Comparator<AttendanceUserStats>() {
                @Override
                public int compare(AttendanceUserStats attendanceUserStats, AttendanceUserStats t1) {
                    return attendanceUserStats.getId().intValue() - t1.getId().intValue();
                }
            });
            Collections.sort(attendanceEventlist, new Comparator<AttendanceEvent>() {
                @Override
                public int compare(AttendanceEvent attendanceEvent, AttendanceEvent t1) {
                    if((attendanceEvent.getStartDateTime() == null) && (t1.getStartDateTime() == null)) {
                        return 0;
                    } else if (attendanceEvent.getStartDateTime() == null){
                        return -1;
                    } else if (t1.getStartDateTime() == null){
                        return 1;
                    } else{
                        return attendanceEvent.getStartDateTime().compareTo(t1.getStartDateTime());
                    }
                }
            });
            Collections.sort(attendanceEventlist, new Comparator<AttendanceEvent>() {
                @Override
                public int compare(AttendanceEvent attendanceEvent, AttendanceEvent t1) {
                    if((attendanceEvent.getStartDateTime() == null) && (t1.getStartDateTime() == null)) {
                        return attendanceEvent.getName().length() - (t1.getName().length());
                    } else{
                        return 0;
                    }
                }
            });
            Collections.sort(attendanceEventlist, new Comparator<AttendanceEvent>() {
                @Override
                public int compare(AttendanceEvent attendanceEvent, AttendanceEvent t1) {
                    if((attendanceEvent.getName().length() - t1.getName().length()) == 0) {
                        return attendanceEvent.getName().compareTo(t1.getName());
                    } else{
                        return 0;
                    }
                }
            });
            eventCount = attendanceEventlist.size();
            studentCount = userStatsList.size();
            header.add("StudentID");
            header.add("Student Name");
            header.add("Section");

            for(int y = 0; y < eventCount; y++){
                if (String.valueOf(attendanceEventlist.get(y).getStartDateTime()).equals(null)){
                    header.add(attendanceEventlist.get(y).getName() + "[ ]");
                    if(commentsOnOff){
                        header.add(attendanceEventlist.get(y).getName() + "[ ]Comments");
                    }
                }
                else{
                    header.add(attendanceEventlist.get(y).getName() + "[" + String.valueOf(attendanceEventlist.get(y).getStartDateTime()) + "]" + "(" + String.valueOf(attendanceEventlist.get(y).getId())+ ")");
                    if(commentsOnOff) {
                        header.add(attendanceEventlist.get(y).getName() + "[" + String.valueOf(attendanceEventlist.get(y).getStartDateTime()) + "]Comments" + "(" + String.valueOf(attendanceEventlist.get(y).getId())+ ")");
                    }
                }
                eventHolder.add(attendanceLogic.getAttendanceEvent(attendanceEventlist.get(y).getId()));
            }
            HSSFFont boldFont = wb.createFont();
            boldFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
            boldFont.setUnderline(HSSFFont.U_SINGLE);
            HSSFCellStyle boldStyle = wb.createCellStyle();
            boldStyle.setFont(boldFont);

            // Create the Header row
            HSSFRow headerRow = mainSheet.createRow(0);
            for (int i = 0; i < header.size(); i++) {
                HSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(header.get(i));
                cell.setCellType(cell.CELL_TYPE_STRING);
                cell.setCellStyle(boldStyle);
            }

            final int[] rowCount = {1};
            final int[] cellCount = {0};
            for(int x = 0; x < studentCount; x++) {
                rowCounter = 0;
                List<AttendanceRecord> attendanceRecordlist = attendanceLogic.getAttendanceRecordsForUser(userStatsList.get(x).getUserID().toString());
                HSSFRow row = mainSheet.createRow(rowCount[0]);
                final User user = sakaiProxy.getUser(userStatsList.get(x).getUserID());
                cellCount[0] = 0;

                if (true) {
                    HSSFCell cell = row.createCell(cellCount[0]);
                    cell.setCellValue(user.getEid());
                    cell.setCellType(cell.CELL_TYPE_STRING);
                    cellCount[0]++;
                }
                if (true) {
                    HSSFCell cell = row.createCell(cellCount[0]);
                    cell.setCellValue(user.getSortName());
                    cell.setCellType(cell.CELL_TYPE_STRING);
                    cellCount[0]++;
                }
                if (true) {
                    HSSFCell cell = row.createCell(cellCount[0]);
                    cell.setCellValue(String.valueOf(sakaiProxy.getCurrentSiteId()));
                    cell.setCellType(cell.CELL_TYPE_STRING);
                    cellCount[0]++;
                }
                for(int y = 0; y < eventCount; y++){
                    if (true) {
                        for(int p = 0; p < eventCount; p++){
                            if(String.valueOf(eventHolder.get(y)).equals(String.valueOf(attendanceRecordlist.get(p).getAttendanceEvent()))){
                                columnFinder = p;
                            }
                        }
                        this.holder = String.valueOf(attendanceRecordlist.get(columnFinder).getStatus());
                        if(this.holder.equals("PRESENT")) {
                            this.holder = "P";
                        } else if (this.holder.equals("UNEXCUSED_ABSENCE")){
                            this.holder = "A";
                        } else if (this.holder.equals("EXCUSED_ABSENCE")){
                            this.holder = "E";
                        } else if (this.holder.equals("LATE")){
                            this.holder = "L";
                        } else if (this.holder.equals("LEFT_EARLY")){
                            this.holder = "LE";
                        } else {
                            this.holder = "N/A";
                        }
                        HSSFCell cell = row.createCell(cellCount[0]);
                        if(blankSheet){
                            cell.setCellValue("");
                        }else{
                            cell.setCellValue(this.holder);
                        }
                        cell.setCellType(cell.CELL_TYPE_STRING);
                        cellCount[0]++;
                    }
                    if(commentsOnOff) {
                        if (true) {
                            this.holder = String.valueOf(attendanceRecordlist.get(columnFinder).getComment());
                            if (this.holder.equals("null")){
                                this.holder = "";
                            }
                            HSSFCell cell = row.createCell(cellCount[0]);
                            if(blankSheet){
                                cell.setCellValue("");
                            }else{
                                cell.setCellValue(this.holder);
                            }
                            cell.setCellType(cell.CELL_TYPE_STRING);
                            cellCount[0]++;
                        }
                    }
                }
                rowCount[0]++;
                this.rowCounter++;
            }
            FileOutputStream fos = new FileOutputStream(tempFile);
            wb.write(fos);

            fos.close();
            wb.close();

        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return tempFile;
    }

    private String buildFileNamePrefix() {
        final String prefix = "attendence_Export-";
        return prefix;
    }

    private String buildFileNameSuffix() {
        return "." + ExportPage.ExportFormat.XLS.toString().toLowerCase();
    }

    private class UploadForm extends Form<Void> {

        FileUploadField fileUploadField;

        public UploadForm(final String id) {
            super(id);

            setMultiPart(true);
            setMaxSize(Bytes.megabytes(2));

            this.fileUploadField = new FileUploadField("upload");
            add(this.fileUploadField);

            add(new Button("continuebutton"));
            SubmitLink submit = new SubmitLink("submitLink");
            add(submit);
        }

        @Override
        public void onSubmit() {
            String statusInput;
            String idHolder;
            String comment;
            int sheetLengthcounter;
            int indexCounter = 0;
            int index = 0;
            int eventCounter = 3;
            boolean noComments = false;
            final String selectedGroup = null;
            final List<Long> idTracker = new ArrayList<Long>();
            List<AttendanceUserStats> userStatsList = attendanceLogic.getUserStatsForCurrentSite(selectedGroup);
            Collections.sort(userStatsList, new Comparator<AttendanceUserStats>() {
                @Override
                public int compare(AttendanceUserStats attendanceUserStats, AttendanceUserStats t1) {
                    return attendanceUserStats.getId().intValue() - t1.getId().intValue();
                }
            });
            AttendanceSite attendanceSite = attendanceLogic.getAttendanceSite(sakaiProxy.getCurrentSiteId());
            List<AttendanceEvent> attendanceEventlist = attendanceLogic.getAttendanceEventsForSite(attendanceSite);
            List<AttendanceUserStats> userList = attendanceLogic.getUserStatsForCurrentSite(selectedGroup);
            int eventCount = attendanceEventlist.size();
            int studentCount = userList.size();
            final FileUpload upload = this.fileUploadField.getFileUpload();

            if (upload != null) {
                try{
                    File temp = upload.writeToTempFile();
                    FileInputStream fis = new FileInputStream(temp);
                    HSSFWorkbook workbook = new HSSFWorkbook(fis);
                    HSSFSheet sheet = workbook.getSheetAt(0);
                    Iterator rows = sheet.rowIterator();
                    rowCounter = 0;
                    for(int r =0; r <= studentCount; r++){
                        sheetLengthcounter = 0;
                        HSSFRow row = (HSSFRow) rows.next();
                        Iterator cells = row.cellIterator();

                        List data = new ArrayList();
                        while (cells.hasNext()) {
                            HSSFCell cell = (HSSFCell) cells.next();
                            data.add(cell);
                            sheetLengthcounter++;
                        }
                        if(sheetLengthcounter < ((eventCount * 2)+ 3)){
                            noComments = true;
                        }
                        if(noComments){
                            eventCounter = (sheetLengthcounter - 3);
                        } else {
                            eventCounter = ((sheetLengthcounter - 3)/2);
                        }
                        if(rowCounter == 0){
                            for(int q =0; q < eventCounter  && q < eventCount; q++) {
                                if(noComments){
                                    idHolder = String.valueOf(data.get(3 + q));
                                }else {
                                    idHolder = String.valueOf(data.get(3 + (2 * q)));
                                }
                                index = idHolder.lastIndexOf(")");
                                idHolder = idHolder.substring(0,index);
                                index = idHolder.lastIndexOf("(");
                                idHolder = idHolder.substring(index + 1);
                                idTracker.add(Long.parseLong(idHolder));
                            }
                        }
                        if (rowCounter > 0){
                            String sheetName = String.valueOf(data.get(1));
                            List<AttendanceRecord> attendanceRecordlist = attendanceLogic.getAttendanceRecordsForUser(userStatsList.get(rowCounter -1).getUserID().toString());
                            String name = sakaiProxy.getUserSortName(userStatsList.get(rowCounter -1).getUserID());
                            for(int q =0; q < eventCounter  && q < eventCount; q++){
                                List<AttendanceRecord> records = new ArrayList<AttendanceRecord>((attendanceLogic.getAttendanceEvent(idTracker.get(q))).getRecords());
                                for(int s = 0; s < studentCount; s++){
                                    if(sheetName.equals(sakaiProxy.getUserSortName(records.get(s).getUserID()))){
                                        indexCounter = s;
                                    }
                                }
                                AttendanceRecord aR = attendanceLogic.getAttendanceRecord(records.get(indexCounter).getId());
                                if(noComments){
                                    statusInput = String.valueOf(data.get(3 + q));
                                    comment = String.valueOf(aR.getComment());
                                }else {
                                    statusInput = String.valueOf(data.get(3 + (2 * q)));
                                    comment = String.valueOf(data.get(4 + (2 * q)));
                                }
                                Status holder = aR.getStatus();
                                if(statusInput.equals("P") || (statusInput.equals("PRESENT"))) {
                                    aR.setStatus(Status.PRESENT);
                                } else if (statusInput.equals("A") || (statusInput.equals("UNEXCUSED_ABSENCE")) || (statusInput.equals("ABSENT"))){
                                    aR.setStatus(Status.UNEXCUSED_ABSENCE);
                                } else if (statusInput.equals("E") || (statusInput.equals("EXCUSED_ABSENCE")) || (statusInput.equals("EXCUSED"))){
                                    aR.setStatus(Status.EXCUSED_ABSENCE);
                                } else if (statusInput.equals("L") || (statusInput.equals("LATE"))){
                                    aR.setStatus(Status.LATE);
                                } else if (statusInput.equals("LE") || (statusInput.equals("LEFT_EARLY"))){
                                    aR.setStatus(Status.LEFT_EARLY);
                                } else {
                                    aR.setStatus(Status.UNKNOWN);
                                }
                                aR.setComment(comment);
                                boolean updated = attendanceLogic.updateAttendanceRecord(aR, holder);
                            }
                        }
                        rowCounter++;
                    }
                    fis.close();
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
                log.debug("file upload success");
            }
            setResponsePage(new Overview());
        }
    }
}
