package org.Simple-CI;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test suite for {@link Builder}
 */
public class BuilderTest {
    public static final String SUCCEEDCOMMITID = "8700bb922383f6821ab2475635a8606451830511";
    public static final String BRANCH_ORIGIN_MASTER = "origin/master";
    public static final String GRADLE_SCAN_URI = "https://gradle.com/s/";
    final TemporaryFolder testRootFolder = new TemporaryFolder();
    static final String REMOTE_REPO_FULL_NAME = "xmas92/BasicJavaGradle";
    File sourceRepo;
    File testLocalRepo;
    File testRemoteRepo;
    String gitURI;
    Builder builder;

    /**
     * https://www.journaldev.com/960/java-unzip-file-example
     * Had to fix for directories
     */
    private static void unzip(String zipFilePath, String destDir) {
        File dir = new File(destDir);
        // create output directory if it doesn't exist
        if (!dir.exists()) dir.mkdirs();
        //buffer for read and write data to file
        byte[] buffer = new byte[1024];
        try (FileInputStream fis = new FileInputStream(zipFilePath)) {
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                if (ze.isDirectory()) {
                    zis.closeEntry();
                    ze = zis.getNextEntry();
                    continue;
                }
                String fileName = ze.getName();
                File newFile = new File(destDir + File.separator + fileName);

                //create directories for sub directories in zip
                new File(newFile.getParent()).mkdirs();

                int len;
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                //close this ZipEntry
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            //close last ZipEntry
            zis.closeEntry();
            zis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Before
    public void setUp() throws IOException {
        builder = new Builder();
        testRootFolder.create();
        sourceRepo = testRootFolder.newFolder("source");
        testLocalRepo = testRootFolder.newFolder("local");
        testRemoteRepo = testRootFolder.newFolder("remote");
        unzip(this.getClass().getClassLoader().getResource("Test.zip").getPath(),
                sourceRepo.getCanonicalPath());
        gitURI = sourceRepo.getAbsolutePath() + File.separator + "Test";
    }

    private BuildData getBuildDataSucceed() {
        return new BuildData(REMOTE_REPO_FULL_NAME, new Date(), SUCCEEDCOMMITID);
    }

    private BuildData getBuildDataFailTest() {
        return new BuildData(REMOTE_REPO_FULL_NAME, new Date(), "a689203af0884de620a14f27b16074dc36c7d521");
    }


    private BuildData getBuildDataFailBuild() {
        return new BuildData(REMOTE_REPO_FULL_NAME, new Date(), "b08a83e1df9eb9e7217b84e68ea0483fa8c6f86c");
    }

    private Message<String> buildMessage(BuildData buildData, String branch) {
        return MessageBuilder.withPayload("")
                .setHeader("buildData", buildData)
                .setHeader("branch", branch)
                .build();
    }

    private BuildData getBuildData(BuildData buildData, String branch) {
        final String USER_DIR = "user.dir";
        String userDir = System.getProperty(USER_DIR);
        System.setProperty(USER_DIR, testRemoteRepo.getAbsolutePath());
        Message<String> msg = builder.build(buildMessage(buildData, branch));
        System.setProperty(USER_DIR, userDir);
        return (BuildData) msg.getHeaders().get("buildData");
    }

    /**
     * Runs a complete clone, build and test of a remote repo. That succeeds
     */
    //@Test //Only run locally
    public void testRemoteRepoSucceed() {
        BuildData buildData = getBuildDataSucceed();
        String branch = BRANCH_ORIGIN_MASTER;
        BuildData retBuildData = getBuildData(buildData, branch);

        assertEquals(BuildStatus.success, retBuildData.getBuildStatus());

        assertEquals(retBuildData.getRepoFullName(), buildData.getRepoFullName());
        assertEquals(retBuildData.getSha(), buildData.getSha());
        assertEquals(retBuildData.getDateCreated(), buildData.getDateCreated());
    }

    /**
     * Runs a complete clone, build and test of a remote repo. That fails build
     */
    //@Test //Only run locally
    public void testRemoteRepoFailBuild() {
        BuildData buildData = getBuildDataFailBuild();
        String branch = "failBuild";
        BuildData retBuildData = getBuildData(buildData, branch);

        assertEquals(BuildStatus.failure, retBuildData.getBuildStatus());

        assertEquals(buildData.getRepoFullName(), retBuildData.getRepoFullName());
        assertEquals(buildData.getSha(), retBuildData.getSha());
        assertEquals(buildData.getDateCreated(), retBuildData.getDateCreated());
    }

    /**
     * Runs a complete clone, build and test of a remote repo. That fails test
     */
    //@Test //Only run locally
    public void testRemoteRepoFailTest() {
        BuildData buildData = getBuildDataFailTest();
        String branch = "fail";
        BuildData retBuildData = getBuildData(buildData, branch);

        assertEquals(BuildStatus.failure, retBuildData.getBuildStatus());

        assertEquals(buildData.getRepoFullName(), retBuildData.getRepoFullName());
        assertEquals(buildData.getSha(), retBuildData.getSha());
        assertEquals(buildData.getDateCreated(), retBuildData.getDateCreated());
    }

    /**
     * Runs build on invalid github repo.
     */
    //@Test Might not be a valid test see Git API Exceptions
    public void testTransportException() {
        BuildData buildData = new BuildData("xmas92/invalidREPO", new Date(), SUCCEEDCOMMITID);
        String branch = "fail";
        BuildData retBuildData = getBuildData(buildData, branch);

        assertEquals(BuildStatus.error, retBuildData.getBuildStatus());

        assertTrue(retBuildData.getMessage().startsWith("TransportException"));

        assertEquals(buildData.getRepoFullName(), retBuildData.getRepoFullName());
        assertEquals(buildData.getSha(), retBuildData.getSha());
        assertEquals(buildData.getDateCreated(), retBuildData.getDateCreated());
    }

    /**
     * Runs build on invalid github repo.
     */
    //@Test Might not be a valid test see Git API Exceptions
    public void testIllegalRemoteException() {
        BuildData buildData = new BuildData(null, new Date(), SUCCEEDCOMMITID);
        String branch = "fail";
        BuildData retBuildData = getBuildData(buildData, branch);

        assertEquals(BuildStatus.error, retBuildData.getBuildStatus());

        assertTrue(retBuildData.getMessage().startsWith("InvalidRemoteException"));

        assertEquals(buildData.getRepoFullName(), retBuildData.getRepoFullName());
        assertEquals(buildData.getSha(), retBuildData.getSha());
        assertEquals(buildData.getDateCreated(), retBuildData.getDateCreated());
    }

    /**
     * Wrapper for private method.
     */
    String buildAndTest(BuildData buildData, String branch, String sha, String repoFullName, String repoGitURI, File gitPath) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = Builder.class.getDeclaredMethod("buildAndTest", BuildData.class, String.class, String.class, String.class, String.class, File.class);
        method.setAccessible(true);
        return (String) method.invoke(builder, buildData, branch, sha, repoFullName, repoGitURI, gitPath);
    }

    /**
     * Test buildAndTest method on a local repository. That succeeds
     */
    @Test
    public void testBuildAndTestLocalSucceed() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        BuildData buildData = getBuildDataSucceed();
        String url = buildAndTest(buildData, BRANCH_ORIGIN_MASTER, buildData.getSha(), buildData.getRepoFullName(), gitURI, testLocalRepo);
        assertTrue(url.startsWith(GRADLE_SCAN_URI));
        assertEquals(BuildStatus.success, buildData.getBuildStatus());
    }

    /**
     * Test buildAndTest method on a local repository. That fails test
     */
    @Test
    public void testBuildAndTestLocalFailTest() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        BuildData buildData = getBuildDataFailTest();
        String url = buildAndTest(buildData, "fail", buildData.getSha(), buildData.getRepoFullName(), gitURI, testLocalRepo);
        assertTrue(url.startsWith(GRADLE_SCAN_URI));
        assertEquals(BuildStatus.failure, buildData.getBuildStatus());
    }

    /**
     * Test buildAndTest method on a local repository. That fails build
     */
    @Test
    public void testBuildAndTestLocalFailBuild() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        BuildData buildData = getBuildDataFailBuild();
        String url = buildAndTest(buildData, "failBuild", buildData.getSha(), buildData.getRepoFullName(), gitURI, testLocalRepo);
        assertTrue(url.startsWith(GRADLE_SCAN_URI));
        assertEquals(BuildStatus.failure, buildData.getBuildStatus());
    }

    /**
     * Wrapper for private method
     */
    static Git cloneGitRepo(String branch, File gitPath, String repoGitURI, CredentialsProvider cp) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method method = Builder.class.getDeclaredMethod("cloneGitRepo", String.class, File.class, String.class, CredentialsProvider.class);
        method.setAccessible(true);
        return (Git) method.invoke(null, branch, gitPath, repoGitURI, cp);
    }

    /**
     * Test cloneGitRepo invalid CredentialsProvider
     */
    @Test(expected = InvocationTargetException.class)
    public void testCloneGitRepoInvalidCredentialsProvider() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        cloneGitRepo(BRANCH_ORIGIN_MASTER, testLocalRepo, "https://github.com/xmas92/DD2480-CI.git",
                new UsernamePasswordCredentialsProvider("BAD", "PASS"));
    }
}