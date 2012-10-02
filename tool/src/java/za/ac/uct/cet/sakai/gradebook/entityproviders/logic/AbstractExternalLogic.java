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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.FunctionManager;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.email.api.EmailService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.javax.PagingPosition;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.service.gradebook.shared.CommentDefinition;
import org.sakaiproject.service.gradebook.shared.GradeDefinition;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.SiteService.SelectionType;
import org.sakaiproject.site.api.SiteService.SortType;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.ResourceLoader;

/**
 * This is the common parts of the logic which is external to our app logic, this provides isolation
 * of the Sakai system from the app so that the integration can be adjusted for future versions or
 * even other systems without requiring rewriting large parts of the code
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public abstract class AbstractExternalLogic {

    public static final String SCORE_UPDATE_ERRORS = "ScoreUpdateErrors";
    public static final String POINTS_POSSIBLE_UPDATE_ERRORS = "PointsPossibleUpdateErrors";
    public static final String USER_DOES_NOT_EXIST_ERROR = "UserDoesNotExistError";
    public static final String GENERAL_ERRORS = "GeneralErrors";

    public String serverId = "UNKNOWN_SERVER_ID";

    public final static String NO_LOCATION = "noLocationAvailable";

    private final static Log log = LogFactory.getLog(AbstractExternalLogic.class);

    protected AuthzGroupService authzGroupService;

    public void setAuthzGroupService(AuthzGroupService authzGroupService) {
        this.authzGroupService = authzGroupService;
    }

    private EmailService emailService;

    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    protected FunctionManager functionManager;

    public void setFunctionManager(FunctionManager functionManager) {
        this.functionManager = functionManager;
    }

    protected GradebookService gradebookService;

    public void setGradebookService(GradebookService gradebookService) {
        this.gradebookService = gradebookService;
    }

    protected ToolManager toolManager;

    public void setToolManager(ToolManager toolManager) {
        this.toolManager = toolManager;
    }

    protected SecurityService securityService;

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    protected ServerConfigurationService serverConfigurationService;

    public void setServerConfigurationService(ServerConfigurationService serverConfigurationService) {
        this.serverConfigurationService = serverConfigurationService;
    }

    protected SessionManager sessionManager;

    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    protected SiteService siteService;

    public void setSiteService(SiteService siteService) {
        this.siteService = siteService;
    }

    protected UserDirectoryService userDirectoryService;

    public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
        this.userDirectoryService = userDirectoryService;
    }

    public void init() {
        serverId = getConfigurationSetting(AbstractExternalLogic.SETTING_SERVER_ID, serverId);
    }

    /**
     * @return the current location id of the current user
     */
    public String getCurrentLocationId() {
        String location = null;
        try {
            String context = toolManager.getCurrentPlacement().getContext();
            location = context;
            // Site s = siteService.getSite( context );
            // location = s.getReference(); // get the entity reference to the site
        } catch (Exception e) {
            // sakai failed to get us a location so we can assume we are not inside the portal
            return NO_LOCATION;
        }
        if (location == null) {
            location = NO_LOCATION;
        }
        return location;
    }

    /**
     * @param locationId
     *            a unique id which represents the current location of the user (entity reference)
     * @return the title for the context or "--------" (8 hyphens) if none found
     */
    public String getLocationTitle(String locationId) {
        String title = null;
        try {
            Site site = siteService.getSite(locationId);
            title = site.getTitle();
        } catch (IdUnusedException e) {
            log.warn("Cannot get the info about locationId: " + locationId);
            title = "----------";
        }
        return title;
    }

    /**
     * Attempt to authenticate a user given a login name and password
     * @param loginname the login name for the user
     * @param password the password for the user
     * @param createSession if true then a session is established for the user and the session ID is returned,
     * otherwise the session is not created
     * @return the user ID if the user was authenticated 
     * OR session ID if authenticated and createSession is true 
     * OR null if the auth params are invalid
     */
    public String authenticateUser(String loginname, String password, boolean createSession) {
        if ( isBlank(loginname) ) {
            throw new IllegalArgumentException("loginname cannot be blank");
        }
        if (password == null) {
            password = "";
        }
        User u = userDirectoryService.authenticate(loginname, password);
        if (u == null) {
            // auth failed
            return null;
        } else {
            // auth succeeded
            if (createSession) {
                Session s = sessionManager.startSession();
                s.setUserId(u.getId());
                s.setUserEid(u.getEid());
                s.setActive();
                sessionManager.setCurrentSession(s);
                authzGroupService.refreshUser(u.getId());
                return s.getId();
            } else {
                return u.getId();
            }
        }
    }

    /**
     * Validate the session id given and optionally make it the current one
     * @param sessionId a sakai session id
     * @param makeCurrent if true and the session id is valid then it is made the current one
     * @return true if the session id is valid OR false if not
     */
    public boolean validateSessionId(String sessionId, boolean makeCurrent) {
        try {
            // this also protects us from null pointer where session service is not set or working
            Session s = sessionManager.getSession(sessionId);
            if (s != null && s.getUserId() != null) {
                if (makeCurrent) {
                    s.setActive();
                    sessionManager.setCurrentSession(s);
                    authzGroupService.refreshUser(s.getUserId());
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failure attempting to set sakai session id ("+sessionId+"): " + e.getMessage());
        }
        return true;
   }

    /**
     * @return the current sakai user session id OR null if none
     */
    public String getCurrentSessionId() {
        String sessionId = null;
        Session s = sessionManager.getCurrentSession();
        if (s != null) {
            sessionId = s.getId();
        }
        return sessionId;
    }

    /**
     * @return the current sakai user id (not username)
     */
    public String getCurrentUserId() {
        return sessionManager.getCurrentSessionUserId();
    }

    /**
     * @return the current Locale as Sakai understands it
     */
    public Locale getCurrentLocale() {
        return new ResourceLoader().getLocale();
    }

    /**
     * Get the display name for a user by their unique id
     * 
     * @param userId
     *            the current sakai user id (not username)
     * @return display name (probably firstname lastname) or "----------" (10 hyphens) if none found
     */
    public String getUserDisplayName(String userId) {
        String name = null;
        try {
            name = userDirectoryService.getUser(userId).getDisplayName();
        } catch (UserNotDefinedException e) {
            log.warn("Cannot get user displayname for id: " + userId);
            name = "--------";
        }
        return name;
    }

    public za.ac.uct.cet.sakai.gradebook.entityproviders.logic.User getUser(String userId) {
        za.ac.uct.cet.sakai.gradebook.entityproviders.logic.User user = null;
        User u = null;
        try {
            u = userDirectoryService.getUser(userId);
        } catch (UserNotDefinedException e) {
            try {
                u = userDirectoryService.getUserByEid(userId);
            } catch (UserNotDefinedException e1) {
                log.warn("Cannot get user for id: " + userId);
            }
        }
        if (u != null) {
            user = new za.ac.uct.cet.sakai.gradebook.entityproviders.logic.User(u.getId(),
                    u.getEid(), u.getDisplayName(), u.getSortName(), u.getEmail());
            user.fname = u.getFirstName();
            user.lname = u.getLastName();
        }
        return user;
    }

    /**
     * @return the system email address or null if none available
     */
    public String getNotificationEmail() {
        // attempt to get the email address, if it is not there then we will not send an email
        String emailAddr = serverConfigurationService.getString("portal.error.email",
                serverConfigurationService.getString("mail.support") );
        if ("".equals(emailAddr)) {
            emailAddr = null;
        }
        return emailAddr;
    }

    /**
     * Sends an email to a group of email addresses
     * @param fromEmail [OPTIONAL] from email
     * @param toEmails array of emails to send to, must not be null or empty
     * @param subject the email subject
     * @param body the body (content) of the email message
     */
    public void sendEmails(String fromEmail, String[] toEmails, String subject, String body) {
        if (toEmails == null || toEmails.length == 0) {
            throw new IllegalArgumentException("toEmails must be set");
        }
        if (fromEmail == null || "".equals(fromEmail)) {
            fromEmail = "\"<no-reply@" + serverConfigurationService.getServerName() + ">";
        }
        for (String emailAddr : toEmails) {
            try {
                emailService.send(fromEmail, emailAddr, subject, body, emailAddr, null, null);
            } catch (Exception e) {
                log.warn("Failed to send email to "+emailAddr+" ("+subject+"): " + e, e);
            }
        }
    }

    /**
     * Check if this user has super admin access
     * 
     * @param userId
     *            the internal user id (not username)
     * @return true if the user has admin access, false otherwise
     */
    public boolean isUserAdmin(String userId) {
        return securityService.isSuperUser(userId);
    }

    /**
     * Check if a user has a specified permission within a context, primarily a convenience method
     * and passthrough
     * 
     * @param userId
     *            the internal user id (not username)
     * @param permission
     *            a permission string constant
     * @param locationId
     *            a unique id which represents the current location of the user (entity reference)
     * @return true if allowed, false otherwise
     */
    public boolean isUserAllowedInLocation(String userId, String permission, String locationId) {
        if (securityService.unlock(userId, permission, locationId)) {
            return true;
        }
        return false;
    }

    /**
     * Get all the courses for the current user, note that this needs to be limited from
     * outside this method for security
     * 
     * @param siteId
     *            [OPTIONAL] limit the return to just this one site
     * @return the sites (up to 100 of them) which the user has instructor access in
     */
    public List<Course> getCoursesForInstructor(String siteId) {
        List<Course> courses = new Vector<Course>();
        if (siteId == null || "".equals(siteId)) {
            List<Site> sites = getInstructorSites();
            for (Site site : sites) {
                courses.add(new Course(site.getId(), site.getTitle(), site.getShortDescription()));
            }
        } else {
            // return a single site and enrollments
            if (siteService.siteExists(siteId)) {
                if (siteService.allowUpdateSite(siteId) || siteService.allowViewRoster(siteId)) {
                    Site site;
                    try {
                        site = siteService.getSite(siteId);
                        Course c = new Course(site.getId(), site.getTitle(), site
                                .getShortDescription());
                        courses.add(c);
                    } catch (IdUnusedException e) {
                        site = null;
                    }
                }
            }
        }
        return courses;
    }

    private List<Site> getInstructorSites() {
        // return a max of 100 sites
        List<Site> instSites = new ArrayList<Site>();
        List<Site> sites = siteService.getSites(SelectionType.UPDATE, null, null, null,
                SortType.TITLE_ASC, new PagingPosition(1, 100));
        for (Site site : sites) {
            // filter out admin sites
            String sid = site.getId();
            if (sid.startsWith("!") || sid.endsWith("Admin") || sid.equals("mercury")) {
                if (log.isDebugEnabled()) {
                    log.debug("Skipping site (" + sid + ") for current user in instructor courses");
                }
                continue;
            }
            instSites.add(site);
        }
        return instSites;
    }

    /**
     * Get the listing of students from the site gradebook, uses GB security so safe to call
     * 
     * @param siteId
     *            the id of the site to get students from
     * @return the list of Students
     */
    @SuppressWarnings("unchecked")
    public List<Student> getStudentsForCourse(String siteId) {
        List<Student> students = new ArrayList<Student>();
        /*** this only works in the post-2.5 gradebook -AZ */
        // Let the gradebook tell use how it defines the students The gradebookUID is the siteId
        String gbID = siteId;
        if (!gradebookService.isGradebookDefined(gbID)) {
            throw new IllegalArgumentException("No gradebook found for course (" + siteId
                    + "), gradebook must be installed in each course to use with iClicker");
        }
        Map<String, String> studentToPoints = gradebookService.getFixedPoint(gbID);
        ArrayList<String> eids = new ArrayList<String>(studentToPoints.keySet());
        Collections.sort(eids);
        for (String eid : eids) {
            try {
                User user = userDirectoryService.getUserByEid(eid);
                students.add(new Student(user.getId(), user.getEid(), user.getDisplayName()));
            } catch (UserNotDefinedException e) {
                log.warn("Undefined user eid (" + eid + ") from site/gb (" + siteId + "): " + e);
            }
        }
        return students;
    }

    /**
     * @param userId
     *            the current sakai user id (not username)
     * @return true if the user has update access in any sites
     */
    public boolean isUserInstructor(String userId) {
        boolean inst = false;
        // admin never counts as in instructor
        if (!isUserAdmin(userId)) {
            int count = siteService.countSites(SelectionType.UPDATE, null, null, null);
            inst = (count > 0);
        }
        return inst;
    }

    /**
     * Check if the current user in an instructor for the given user id,
     * this will return the first course found in alpha order,
     * will only check the first 100 courses
     * 
     * @param studentUserId the Sakai user id for the student
     * @return the course ID of the course they are an instructor for the student OR null if they are not
     */
    public String isInstructorOfUser(String studentUserId) {
        if (studentUserId == null || "".equals(studentUserId)) {
            throw new IllegalArgumentException("studentUserId must be set");
        }
        String courseId = null;
        List<Site> sites = getInstructorSites();
        if (sites != null && ! sites.isEmpty()) {
            if (sites.size() >= 99) {
                // if instructor of 99 or more sites then auto-approved 
                courseId = sites.get(0).getId();
            } else {
                for (Site site : sites) {
                    Member member = site.getMember(studentUserId);
                    if (member != null) {
                        courseId = site.getId();
                        break;
                    }
                }
            }
        }
        return courseId;
    }

    /**
     * Gets the gradebook data for a given site, this uses the gradebook security so it is safe for
     * anyone to call
     * 
     * @param siteId a sakai siteId (cannot be group Id)
     * @param gbItemName [OPTIONAL] an item name to fetch from this gradebook (limit to this item only),
     * if null then all items are returned
     */
    @SuppressWarnings("unchecked")
    public Gradebook getCourseGradebook(String siteId, String gbItemName) {
        // The gradebookUID is the siteId, the gradebookID is a long
        String gbID = siteId;
        if (!gradebookService.isGradebookDefined(gbID)) {
            throw new IllegalArgumentException("No gradebook found for site: " + siteId);
        }
        // verify permissions
        String userId = getCurrentUserId();
        if (userId == null 
                || ! securityService.unlock("gradebook.gradeAll", siteService.siteReference(siteId)) 
                 ) {
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

    	// This is the post 2.5 way
    	List<GradeDefinition> grades = gradebookService.getGradesForStudentsForItem(gbID,
    			assignment.getId(), studentIds);
    	for (GradeDefinition gd : grades) {
    		String studId = gd.getStudentUid();
    		String studEID = studentUserIds.get(studId);
    		GradebookItemScore score = new GradebookItemScore(assignment.getId().toString(),
    				studId, gd.getGrade(), studEID, gd.getGraderUid(), gd.getDateRecorded(),
    				gd.getGradeComment());
    		gbItem.scores.add(score);
    	}
    	return gbItem;
    }

    /**
     * Save a gradebook item and optionally the scores within <br/>
     * Scores must have at least the studentId or username AND the grade set
     * 
     * @param gbItem
     *            the gradebook item to save, must have at least the gradebookId and name set
     * @return the updated gradebook item and scores, contains any errors that occurred
     * @throws IllegalArgumentException if the assignment is invalid and cannot be saved
     * @throws SecurityException if the current user does not have permissions to save
     */
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
        try {
            // try to save or update it
            if (assignment == null) {
                // no item so create one
                assignment = new Assignment();
                assignment.setExternallyMaintained(false); // cannot modify it later if true
                // assign values
                assignment.setDueDate(gbItem.dueDate);
                assignment.setExternalAppName(gbItem.type);
                //assignment.setExternalId(gbItem.type);
                assignment.setName(gbItem.name);
                assignment.setPoints(gbItem.pointsPossible);
                assignment.setReleased(gbItem.released);
                gradebookService.addAssignment(gradebookUid, assignment);
                // SecurityException, AssignmentHasIllegalPointsException, RuntimeException
            } else {
                assignment.setExternallyMaintained(false); // cannot modify it later if true
                // assign new values to existing assignment
                if (gbItem.dueDate != null) {
                    assignment.setDueDate(gbItem.dueDate);
                }
                if (gbItem.type != null) {
                    assignment.setExternalAppName(gbItem.type);
                    //assignment.setExternalId(gbItem.type);
                }
                if (gbItem.pointsPossible != null && gbItem.pointsPossible >= 0d) {
                    assignment.setPoints(gbItem.pointsPossible);
                }
                // assignment.setReleased(gbItem.released); // no mod released setting from here
                gradebookService.updateAssignment(gradebookUid, assignment.getName(), assignment);
                // SecurityException, RuntimeException
            }
        } catch (SecurityException e) {
            throw e; // rethrow
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid assignment ("+assignment+"): cannot create: " + e, e);
        }
        gbItem.id = assignment.getId()+""; // avoid NPE
        int errorsCount = 0;
        if (gbItem.scores != null && !gbItem.scores.isEmpty()) {
            // now update scores if there are any to update, 
            // this will not remove scores and will only add new ones
            for (GradebookItemScore score : gbItem.scores) {
                if (isBlank(score.username) && isBlank(score.userId)) {
                    score.error = USER_DOES_NOT_EXIST_ERROR; //"USER_MISSING_ERROR";
                    continue;
                }
                String studentId = score.userId;
                if (studentId == null || "".equals(studentId)) {
                    // convert student EID to ID
                    try {
                        studentId = userDirectoryService.getUserId(score.username);
                        score.userId = studentId;
                    } catch (UserNotDefinedException e) {
                        score.error = USER_DOES_NOT_EXIST_ERROR;
                        errorsCount++;
                        continue;
                    }
                } else {
                    // validate the student ID
                    try {
                        score.username = userDirectoryService.getUserEid(studentId);
                    } catch (UserNotDefinedException e) {
                        score.error = USER_DOES_NOT_EXIST_ERROR;
                        errorsCount++;
                        continue;
                    }
                }
                score.assignId(gbItem.id, studentId);
                // null/blank scores are not allowed
                if (isBlank(score.grade)) {
                    score.error = "NO_SCORE_ERROR";
                    errorsCount++;
                    continue;
                }

                Double dScore;
                try {
                    dScore = Double.valueOf(score.grade);
                } catch (NumberFormatException e) {
                    score.error = "SCORE_INVALID";
                    errorsCount++;
                    continue;
                }
                // Student Score should not be greater than the total points possible
                if (dScore > assignment.getPoints()) {
                    score.error = POINTS_POSSIBLE_UPDATE_ERRORS;
                    errorsCount++;
                    continue;
                }
                try {
                    // check against existing score
                    Double currentScore = gradebookService.getAssignmentScore(gradebookUid, gbItem.name, studentId);
                    if (currentScore != null) {
                        if (dScore < currentScore) {
                            score.error = SCORE_UPDATE_ERRORS;
                            errorsCount++;
                            continue;
                        }
                    }
                    // null grade deletes the score
                    gradebookService.setAssignmentScore(gradebookUid, gbItem.name, studentId, dScore, "i>clicker");
                    if (score.comment != null && ! "".equals(score.comment)) {
                        gradebookService.setAssignmentScoreComment(gradebookUid, gbItem.name, studentId, score.comment);
                    }
                } catch (Exception e) {
                    // General errors, caused while performing updates (Tag: generalerrors)
                    log.warn("Failure saving score ("+score+"): "+e);
                    score.error = GENERAL_ERRORS;
                    errorsCount++;
                }
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
            if (errorsCount > 0) {
                gbItem.scoreErrors = new HashMap<String, String>();
                for (GradebookItemScore score : gbItem.scores) {
                    gbItem.scoreErrors.put(score.id, score.error);
                }
            }
        }
        return gbItem;
    }

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

    /**
     * String type: gets the printable name of this server
     */
    public static String SETTING_SERVER_NAME = "server.name";
    /**
     * String type: gets the unique id of this server (safe for clustering if used)
     */
    public static String SETTING_SERVER_ID = "server.cluster.id";
    /**
     * String type: gets the URL to this server
     */
    public static String SETTING_SERVER_URL = "server.main.URL";
    /**
     * String type: gets the URL to the portal on this server (or just returns the server URL if no
     * portal in use)
     */
    public static String SETTING_PORTAL_URL = "server.portal.URL";
    /**
     * Boolean type: if true then there will be data preloads and DDL creation, if false then data
     * preloads are disabled (and will cause exceptions if preload data is missing)
     */
    public static String SETTING_AUTO_DDL = "auto.ddl";

    /**
     * Retrieves settings from the configuration service (sakai.properties)
     * 
     * @param settingName
     *            the name of the setting to retrieve, Should be a string name: e.g. auto.ddl,
     *            mystuff.config, etc. OR one of the SETTING constants (e.g
     *            {@link #SETTING_AUTO_DDL})
     * 
     * @param defaultValue
     *            a specified default value to return if this setting cannot be found, <b>NOTE:</b>
     *            You can set the default value to null but you must specify the class type in
     *            parens
     * @return the value of the configuration setting OR the default value if none can be found
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfigurationSetting(String settingName, T defaultValue) {
        T returnValue = defaultValue;
        if (SETTING_SERVER_NAME.equals(settingName)) {
            returnValue = (T) serverConfigurationService.getServerName();
        } else if (SETTING_SERVER_URL.equals(settingName)) {
            returnValue = (T) serverConfigurationService.getServerUrl();
        } else if (SETTING_PORTAL_URL.equals(settingName)) {
            returnValue = (T) serverConfigurationService.getPortalUrl();
        } else if (SETTING_SERVER_ID.equals(settingName)) {
            returnValue = (T) serverConfigurationService.getServerIdInstance();
        } else {
            if (defaultValue == null) {
                returnValue = (T) serverConfigurationService.getString(settingName);
                if ("".equals(returnValue)) {
                    returnValue = null;
                }
            } else {
                if (defaultValue instanceof Number) {
                    int num = ((Number) defaultValue).intValue();
                    int value = serverConfigurationService.getInt(settingName, num);
                    returnValue = (T) Integer.valueOf(value);
                } else if (defaultValue instanceof Boolean) {
                    boolean bool = ((Boolean) defaultValue).booleanValue();
                    boolean value = serverConfigurationService.getBoolean(settingName, bool);
                    returnValue = (T) Boolean.valueOf(value);
                } else if (defaultValue instanceof String) {
                    returnValue = (T) serverConfigurationService.getString(settingName,
                            (String) defaultValue);
                }
            }
        }
        return returnValue;
    }

    public static boolean isBlank(String str) {
        if (str == null || "".equals(str)) {
            return true;
        }
        return false;
    }

    // METHODS TO ADD TOOL TO MY WORKSPACES

    static String[] SPECIAL_USERS = {"admin","postmaster"};

    /**
     * Set a current user for the current thread, create session if needed
     * @param userId the userId to set
     */
    public void setCurrentUser(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        Session currentSession = sessionManager.getCurrentSession();
        if (currentSession == null) {
            // start a session if none is around
            currentSession = sessionManager.startSession(userId);
        }
        currentSession.setUserId(userId);
        currentSession.setActive();
        sessionManager.setCurrentSession(currentSession);
        authzGroupService.refreshUser(userId);
    }

  

}
