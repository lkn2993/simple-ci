package org.Simple-CI;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * BuildData object contains all information about at build
 */
public class BuildData {
    private final Logger logger = LoggerFactory.getLogger(BuildData.class);
    private final String repoFullName;
    private final Date dateCreated;
    private Date dateStart;
    private Date dateFinish;
    private final String sha;
    private ArrayList<String> buildLog;
    private BuildStatus buildStatus;
    private String message;

    /**
     * Create a build with the basic data that should always be available.
     * @param repoFullName the full name of the GitHub repo. E.g "xmas92/test-project"
     * @param dateCreated the date the build was first created
     * @param sha the sha1 hash that represents the
     */
    public BuildData(String repoFullName, Date dateCreated, String sha) {
        this.repoFullName = repoFullName;
        this.dateCreated = dateCreated;
        if (!sha.matches("^[a-z0-9]{40}$"))
            logger.error("sha string:[" + sha + "] does not match regex ^[a-z0-9]{40}$");
        this.sha = sha;
    }

    /**
     * Getter for GitHub repo name in the form of "xmas92/test-project"
     * @return GitHub repo name
     */
    public String getRepoFullName() {
        return repoFullName;
    }

    /**
     * Getter for date and time the build started
     * @return start date
     */
    public Date getDateStart() {
        return dateStart;
    }

    /**
     * Getter for  date and time the build was created
     * @return create date
     */
    public Date getDateCreated() {
        return dateCreated;
    }

    /**
     * Setter for the date and the the build started
     * @param dateStart build start date
     */
    public void setDateStart(Date dateStart) {
        if (dateStart != null && dateStart.before(dateCreated))
            logger.error("Invariant !dateStart.before(dateCreated) not true");
        this.dateStart = dateStart;
    }

    /**
     * Getter for  date and time the build was finished
     * @return finish date
     */
    public Date getDateFinish() {
        return dateFinish;
    }

    /**
     * Setter for the date and the the build finished
     * @param dateFinish build finish date
     */
    public void setDateFinish(Date dateFinish) {
        if (dateFinish != null && dateFinish.before(dateCreated))
            logger.error("Invariant !dateFinish.before(dateCreated) not true");
        if (dateFinish != null && dateStart == null)
            logger.error("Adding dateFinish before dateStart");
        else if (dateStart != null && dateFinish != null && dateFinish.before(dateStart))
            logger.error("Invariant !dateFinish.before(dateStart) not true");
        this.dateFinish = dateFinish;
    }

    /**
     * Getter for the commit sha1 hash
     * @return sha1 has of the commit
     */
    public String getSha() {
        return sha;
    }

    /**
     * Getter for the build log. Every entry is one output line.
     * @return build log
     */
    public List<String> getBuildLog() {
        return buildLog;
    }

    /**
     * Setter for the build log. Every entry is one output line.
     * @param buildLog build log
     */
    public void setBuildLog(List<String> buildLog) {
        this.buildLog = new ArrayList<String>(buildLog);
    }

    /**
     * Getter for the build status. Equivalent to the GitHub commit statuses.
     * @return build status
     */
    public BuildStatus getBuildStatus() {
        return buildStatus;
    }

    /**
     * Setter for the build status. Equivalent to the GitHub commit statuses.
     * @param buildStatus build status
     */
    public void setBuildStatus(BuildStatus buildStatus) {
        this.buildStatus = buildStatus;
    }

    /**
     * Getter for the build message. Displayed on GitHub-
     * @return build message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Setter for the build message.
     * @param message build message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Unique hashCode based on sha and dateCreated
     * @return Unique hashCode
     */
    @Override
    public int hashCode() {
        return Objects.hash(sha, dateCreated);
    }

    /**
     * Checks if obj and this BuildData have the same logical state.
     * @param obj other object
     * @return true if same logical state, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || !(obj instanceof BuildData)) return false;
        BuildData other = (BuildData)obj;
        if (    this.repoFullName.equals(other.repoFullName) &&
                this.sha.equals(other.sha) &&
                this.dateCreated.equals(other.dateCreated) &&
                ((this.dateStart == null && other.dateStart == null) ||
                        (this.dateStart != null && this.dateStart.equals(other.dateStart))) &&
                ((this.dateFinish == null && other.dateFinish == null) ||
                        (this.dateFinish != null && this.dateFinish.equals(other.dateFinish))) &&
                ((this.buildLog == null && other.buildLog == null) ||
                        (this.buildLog != null && this.buildLog.equals(other.buildLog))) &&
                ((this.buildStatus == null && other.buildStatus == null) ||
                        (this.buildStatus != null && this.buildStatus.equals(other.buildStatus))) &&
                ((this.message == null && other.message == null) ||
                        (this.message != null && this.message.equals(other.message)))
        )
            return true;
        return false;
    }

    /**
     * Creates and returns string containing JSON representation of the BuildData objects
     * @return JSON representation of the BuildData objects
     */
    @Override
    public String toString() {
        return new JSONObject()
                .put("repoFullName", repoFullName)
                .put("dateCreated", dateCreated != null ? dateCreated.toString() : null)
                .put("dateStart", dateStart != null ? dateStart.toString() : null)
                .put("dateFinish", dateFinish != null ? dateFinish.toString() : null)
                .put("sha", sha)
                .put("buildLog", new JSONArray(buildLog))
                .put("buildStatus", buildStatus != null ? buildStatus.name() : null)
                .put("message", message)
                .toString();
    }
}
