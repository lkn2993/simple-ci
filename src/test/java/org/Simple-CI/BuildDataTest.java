package org.Simple-CI;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.Before;
import org.junit.Test;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Test suite for {@link BuildData}
 */
public class BuildDataTest {
    public static final List<String> BUILDLOG = Arrays.asList(new String[]{
            "Line1",
            "Line2",
            "Line3"
    });
    private BuildData buildData;
    private final String repoFullName = "xmas92/test-project";
    private final String sha = "75bb896e1bf493c700d29fd5c0ffeae84682d6ab";
    private final Date dateCreated = new Date();
    private final String message = "message";
    @Before
    public void setUp() {
        // Setup common buildData
        buildData = new BuildData(repoFullName,dateCreated,sha);
    }

    @Test
    public void repoFullNameTest() {
        // test repoFullName setter
        assertEquals(buildData.getRepoFullName(), repoFullName);
    }

    @Test
    public void dateCreatedTest() {
        // test dateCreated setter
        assertEquals(buildData.getDateCreated(), dateCreated);
    }

    @Test
    public void dateStartTest() {
        // test dateStart getter and setter
        Date dateNow = new Date();
        buildData.setDateStart(dateNow);
        assertEquals(buildData.getDateStart(), dateNow);
    }

    @Test
    public void dateFinishTest() {
        // test dateFinish getter and setter
        Date dateNow = new Date();
        buildData.setDateStart(dateNow);
        buildData.setDateFinish(dateNow);
        assertEquals(buildData.getDateFinish(), dateNow);
    }

    @Test
    public void shaTest() {
        // test sha setter
        assertEquals(buildData.getSha(), sha);
    }

    @Test
    public void buildLogTest() {
        // test buildLog getter and setter
        List<String> buildLog = new ArrayList<>(BUILDLOG);
        buildData.setBuildLog(buildLog);
        assertEquals(buildData.getBuildLog(), buildLog);
        assertArrayEquals(buildData.getBuildLog().toArray(), buildLog.toArray());
        buildLog.add("Line4");
        assertNotEquals(buildData.getBuildLog(), buildLog);
    }

    @Test
    public void buildStatusTest() {
        // test buildStatus getter and setter
        BuildStatus buildStatus = BuildStatus.error;
        buildData.setBuildStatus(BuildStatus.error);
        assertEquals(buildData.getBuildStatus(), buildStatus);
    }

    @Test
    public void messageTest() {
        // test message getter and setter
        buildData.setMessage(message);
        assertEquals(buildData.getMessage(), message);
    }

    @Test
    public void hashCodeTest() {
        // test that hashCode override works
        assertEquals(buildData.hashCode(), Objects.hash(sha, dateCreated));
        BuildData buildData1 = new BuildData(repoFullName,dateCreated,sha);
        // test that it only uses sha and dateCreated
        buildData1.setMessage(message);
        assertEquals(buildData.hashCode(), buildData1.hashCode());
    }


    @Test
    public void equals() {
        // test that equals override work
        BuildData buildData1 = new BuildData(repoFullName,dateCreated,sha);
        assertEquals(buildData, buildData1);
        assertEquals(buildData1, buildData);

        // test message different
        buildData1.setMessage(message);
        assertNotEquals(buildData, buildData1);
        assertNotEquals(buildData1, buildData);

        // test buildLog different
        buildData1 = new BuildData(repoFullName,dateCreated,sha);
        buildData1.setBuildLog(Arrays.asList(new String[]{"Line1"}));
        assertNotEquals(buildData, buildData1);
        assertNotEquals(buildData1, buildData);

        // test buildStatus different
        buildData1 = new BuildData(repoFullName,dateCreated,sha);
        buildData1.setBuildStatus(BuildStatus.failure);
        assertNotEquals(buildData, buildData1);
        assertNotEquals(buildData1, buildData);

        // test dateStart different
        buildData1 = new BuildData(repoFullName,dateCreated,sha);
        buildData1.setDateStart(new Date());
        assertNotEquals(buildData, buildData1);
        assertNotEquals(buildData1, buildData);

        // test dateFinish different
        buildData.setDateStart(buildData1.getDateStart());
        buildData1.setDateFinish(new Date());
        assertNotEquals(buildData, buildData1);
        assertNotEquals(buildData1, buildData);
    }

    @Test
    public void loggerTest() {
        // test logger functionality
        Logger logger = (Logger)LoggerFactory.getLogger(BuildData.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        // invalid sha1 string
        String sha1 = sha.substring(0,20);

        buildData = new BuildData(repoFullName, dateCreated, sha1);
        // out of order dates dateFinish before dateStart before dateCreated
        buildData.setDateFinish(new Date(dateCreated.getTime() - 1000));
        buildData.setDateStart(new Date(dateCreated.getTime() - 500));
        buildData.setDateFinish(new Date(dateCreated.getTime() - 1000));

        // capture log and compare output
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("sha string:[" + sha1 + "] does not match regex ^[a-z0-9]{40}$",
                logsList.get(0).getMessage());
        assertEquals(Level.ERROR, logsList.get(0).getLevel());

        assertEquals("Invariant !dateFinish.before(dateCreated) not true",
                logsList.get(1).getMessage());
        assertEquals(Level.ERROR, logsList.get(1).getLevel());

        assertEquals("Adding dateFinish before dateStart",
                logsList.get(2).getMessage());
        assertEquals(Level.ERROR, logsList.get(2).getLevel());

        assertEquals("Invariant !dateStart.before(dateCreated) not true",
                logsList.get(3).getMessage());
        assertEquals(Level.ERROR, logsList.get(3).getLevel());

        assertEquals("Invariant !dateFinish.before(dateStart) not true",
                logsList.get(5).getMessage());
        assertEquals(Level.ERROR, logsList.get(5).getLevel());

    }

    @Test
    public void toStringTest() {
        // test default, the rest null
        assertEquals("{\"dateCreated\":\"" + buildData.getDateCreated().toString() +"\"," +
                        "\"repoFullName\":\"xmas92/test-project\"," +
                        "\"sha\":\"75bb896e1bf493c700d29fd5c0ffeae84682d6ab\"," +
                        "\"buildLog\":[]}",
                buildData.toString());
        // test all data set
        buildData.setBuildStatus(BuildStatus.pending);
        buildData.setMessage("message");
        buildData.setBuildLog(new ArrayList<>(BUILDLOG));
        buildData.setDateStart(new Date());
        buildData.setDateFinish(new Date());
        assertEquals("{\"dateCreated\":\"" + buildData.getDateCreated().toString() + "\"," +
                        "\"dateStart\":\"" + buildData.getDateStart().toString() + "\"," +
                        "\"dateFinish\":\"" + buildData.getDateFinish().toString() + "\"," +
                        "\"message\":\"message\"," +
                        "\"repoFullName\":\"xmas92/test-project\"," +
                        "\"sha\":\"75bb896e1bf493c700d29fd5c0ffeae84682d6ab\"," +
                        "\"buildLog\":[\"Line1\",\"Line2\",\"Line3\"]," +
                        "\"buildStatus\":\"pending\"}",
                buildData.toString());
    }
}