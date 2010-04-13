/**
 * Copyright (c) 2009 i>clicker (R) <http://www.iclicker.com/dnn/>
 *
 * This file is part of i>clicker Sakai integrate.
 *
 * i>clicker Sakai integrate is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * i>clicker Sakai integrate is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with i>clicker Sakai integrate.  If not, see <http://www.gnu.org/licenses/>.
 */
package za.ac.uct.cet.sakai.gradebook.entityproviders.logic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the 2.6 version of the external logic service
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class ExternalLogic extends AbstractExternalLogic {

    private final static Log log = LogFactory.getLog(ExternalLogic.class);

    /**
     * Place any code that should run when this class is initialized by spring here
     */
    public void init() {
        super.init();
        log.info("INIT");
    }




/****************
    public Gradebook getCourseGradebook(String siteId, String gbItemName) {
        // The gradebookUID is the siteId, the gradebookID is a long
        String gbID = siteId;
        if (!gradebookService.isGradebookDefined(gbID)) {
            throw new IllegalArgumentException("No gradebook found for site: " + siteId);
        }
        // verify permissions
        String userId = getCurrentUserId();
        if (userId == null 
                || ! siteService.allowUpdateSite(userId) 
                || ! siteService.allowViewRoster(userId) ) {
            throw new SecurityException("User ("+userId+") cannot access gradebook in site ("+siteId+")");
        }
        Gradebook gb = new Gradebook(gbID);
        gb.students = getStudentsForCourse(siteId);
        Map<String, String> studentUserIds = new ConcurrentHashMap<String, String>();
        for (Student student : gb.students) {
            studentUserIds.put(student.userId, student.username);
        }
        ArrayList<String> studentIds = new ArrayList<String>(studentUserIds.keySet());
        if (gbItemName == null) {
            List<Assignment> gbitems = gradebookService.getAssignments(gbID);
            for (Assignment assignment : gbitems) {
                GradebookItem gbItem = makeGradebookItemFromAssignment(gbID, assignment, studentUserIds, studentIds);
                gb.items.add(gbItem);
            }
        } else {
            Assignment assignment = gradebookService.getAssignment(gbID, gbItemName);
            if (assignment != null) {
                GradebookItem gbItem = makeGradebookItemFromAssignment(gbID, assignment, studentUserIds, studentIds);
                gb.items.add(gbItem);
            } else {
                throw new IllegalArgumentException("Invalid gradebook item name ("+gbItemName+"), no item with this name found in cource ("+siteId+")");
            }
        }
        return gb;
    }

    private GradebookItem makeGradebookItemFromAssignment(String gbID, Assignment assignment,
            Map<String, String> studentUserIds, ArrayList<String> studentIds) {
        // build up the items listing
        GradebookItem gbItem = new GradebookItem(gbID, assignment.getName(), assignment
                .getPoints(), assignment.getDueDate(), assignment.getExternalAppName(),
                assignment.isReleased());
        gbItem.id = assignment.getId().toString();
        // We have to iterate through each student and get the grades out.. 2.5 gradebook service has problems
        for (String studentId : studentIds) {
            // too expensive: if (gradebookService.getGradeViewFunctionForUserForStudentForItem(gbID, assignment.getId(), studentId) == null) {
            Double grade = gradebookService.getAssignmentScore(gbID, assignment.getName(),
                    studentId);
            if (grade != null) {
                GradebookItemScore score = new GradebookItemScore(assignment.getId().toString(),
                        studentId, grade.toString() );
                score.username = studentUserIds.get(studentId);
                CommentDefinition cd = gradebookService.getAssignmentScoreComment(gbID, assignment
                        .getName(), studentId);
                if (cd != null) {
                    score.comment = cd.getCommentText();
                    score.recorded = cd.getDateRecorded();
                    score.graderUserId = cd.getGraderUid();
                }
                gbItem.scores.add(score);
            }
        }
        // This is the post 2.5 way
        // List<GradeDefinition> grades = gradebookService.getGradesForStudentsForItem(siteId,
        // assignment.getId(), studentIds);
        // for (GradeDefinition gd : grades) {
        // String studId = gd.getStudentUid();
        // String studEID = studentUserIds.get(studId);
        // GradebookItemScore score = new GradebookItemScore(assignment.getId().toString(),
        // studId, gd.getGrade(), studEID, gd.getGraderUid(), gd.getDateRecorded(),
        // gd.getGradeComment());
        // gbItem.scores.add(score);
        // }
        return gbItem;
    }

    public GradebookItem saveGradebookItem(GradebookItem gbItem) {
        if (gbItem == null) {
            throw new IllegalArgumentException("gbItem cannot be null");
        }
        if (gbItem.gradebookId == null || "".equals(gbItem.gradebookId)) {
            throw new IllegalArgumentException("gbItem must have the gradebookId set");
        }
        if (gbItem.name == null || "".equals(gbItem.name)) {
            throw new IllegalArgumentException("gbItem must have the name set");
        }
        String gradebookUid = gbItem.gradebookId;
        Assignment assignment = null;
        // find by name
        if (gradebookService.isAssignmentDefined(gradebookUid, gbItem.name)) {
            assignment = gradebookService.getAssignment(gradebookUid, gbItem.name);
        }
        // in the pre-2.6 GB we can only lookup by name
        if (assignment == null) {
            // try to find by name
            if (gradebookService.isAssignmentDefined(gradebookUid, gbItem.name)) {
                assignment = gradebookService.getAssignment(gradebookUid, gbItem.name);
            }
        }
        // now we have the item if it exists
        if (assignment == null) {
            // no item so create one
            assignment = new Assignment();
            assignment.setExternallyMaintained(false); // cannot modify it later if true
            // assign values
            assignment.setDueDate(gbItem.dueDate);
            assignment.setExternalAppName(gbItem.type);
            assignment.setExternalId(gbItem.type);
            assignment.setName(gbItem.name);
            assignment.setPoints(gbItem.pointsPossible);
            assignment.setReleased(gbItem.released);
            gradebookService.addAssignment(gradebookUid, assignment);
        } else {
            assignment.setExternallyMaintained(false); // cannot modify it later if true
            // assign new values to existing assignment
            if (gbItem.dueDate != null) {
                assignment.setDueDate(gbItem.dueDate);
            }
            if (gbItem.type != null) {
                assignment.setExternalAppName(gbItem.type);
                assignment.setExternalId(gbItem.type);
            }
            if (gbItem.pointsPossible != null && gbItem.pointsPossible >= 0d) {
                assignment.setPoints(gbItem.pointsPossible);
            }
            // assignment.setReleased(gbItem.released); // no mod released setting from here
            gradebookService.updateAssignment(gradebookUid, assignment.getName(), assignment);
        }
        gbItem.id = assignment.getId().toString();
        if (gbItem.scores != null && !gbItem.scores.isEmpty()) {
            // now update scores if there are any to update, this will not remove scores and will
            // only add new ones
            Map<GradebookItemScore, String> scoreErrors = new HashMap<GradebookItemScore, String>();
            for (GradebookItemScore score : gbItem.scores) {
                if (isBlank(score.username) && isBlank(score.userId)) {
                    scoreErrors.put(score, "USER_MISSING_ERROR");
                    continue;
                }
                String studentId = score.userId;
                if (studentId == null || "".equals(studentId)) {
                    // convert student EID to ID
                    try {
                        studentId = userDirectoryService.getUserId(score.username);
                        score.userId = studentId;
                    } catch (UserNotDefinedException e) {
                        scoreErrors.put(score, "UserDoesNotExistError");
                        continue;
                    }
                } else {
                    // validate the student ID
                    try {
                        score.username = userDirectoryService.getUserEid(studentId);
                    } catch (UserNotDefinedException e) {
                        scoreErrors.put(score, "UserDoesNotExistError");
                        continue;
                    }
                }
                // null/blank scores are not allowed
                if (isBlank(score.grade)) {
                    scoreErrors.put(score, "NO_SCORE_ERROR");
                    continue;
                }

                Double dScore;
                try {
                    dScore = Double.valueOf(score.grade);
                } catch (NumberFormatException e) {
                    scoreErrors.put(score, "SCORE_INVALID");
                    continue;
                }
                // null grade deletes the score
                gradebookService.setAssignmentScore(gradebookUid, gbItem.name, studentId, dScore, "i>clicker");
                if (score.comment != null && ! "".equals(score.comment)) {
                    gradebookService.setAssignmentScoreComment(gradebookUid, gbItem.name, studentId, score.comment);
                }
                score.assignId(gbItem.id, studentId);
// post-2.5 gradebook method
//                try {
//                    gradebookService.saveGradeAndCommentForStudent(gradebookUid, gbItemId,
//                            studentId, score.grade, score.comment);
//                } catch (InvalidGradeException e) {
//                    scoreErrors.put(score, "SCORE_INVALID");
//                    continue;
//                }
//                GradeDefinition gd = gradebookService.getGradeDefinitionForStudentForItem(
//                        gradebookUid, gbItemId, studentId);
//                score.assignId(gbItem.id, gd.getStudentUid());
//                score.comment = gd.getGradeComment();
//                score.grade = gd.getGrade();
//                score.graderUserId = gd.getGraderUid();
//                score.recorded = gd.getDateRecorded();
            }
            // put the errors in the item
            if (scoreErrors.size() > 0) {
                gbItem.scoreErrors = new HashMap<String, String>();
                for (Entry<GradebookItemScore, String> entry : scoreErrors.entrySet()) {
                    gbItem.scoreErrors.put(entry.getKey().toString(), entry.getValue());
                }
            }
        }
        return gbItem;
    }
******************/
    
    /** Not possible in the sakai 2.5 gradebook service -AZ
    public boolean removeGradebookItem(String siteId, String itemName) {
        boolean removed = false;
        if (gradebookService.isAssignmentDefined(siteId, itemName)) {
            Assignment a = gradebookService.getAssignment(siteId, itemName);
            gradebookService.removeAssignment(a.getId());
        }
        return removed;
    }
    ***/

}
