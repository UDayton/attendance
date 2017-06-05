package org.sakaiproject.attendance.model;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by james on 5/19/17.
 */


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Export implements Serializable {
    private long userID;
    private String Status;
    private String status;
    private String comment;
}