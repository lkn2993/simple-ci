package org.Simple-CI;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;

/**
 * This is the class that takes the repository and builds it on call.
 */
@Component
public class Builder {

    private Logger log2 = LoggerFactory.getLogger(this.getClass().getName());

    public Builder() {
        /**/
    }

    /**
     * Downloads, builds and tests repository defined in the msg header field buildData
     * using branch defined in msg header field branch.
     * @param msg Should contain header field buildData({@link BuildData}) and branch(String)
     * @return msg containing header field buildData({@link BuildData}) and url(String)
     */
    @ServiceActivator(inputChannel = "builderBridge", outputChannel = "buildStatusChannel")
    public Message<String> build(Message<String> msg) {
        log2.info("Enter build function");
        String payload = msg.getPayload();
        BuildData buildData = (BuildData)msg.getHeaders().get("buildData");
        if (buildData == null)
            return MessageBuilder.withPayload(payload)
                    .copyHeadersIfAbsent(msg.getHeaders())
                    .build();
        String branch = (String)msg.getHeaders().get("branch");
        if (branch == null)
            branch = "";
        final String sha = buildData.getSha();
        final String repoFullName = buildData.getRepoFullName();
        final String repoGitURI = "https://github.com/" + repoFullName + ".git";

        File gitPath = new File(System.getProperty("user.dir") + File.separator + "repo");

        // Clone, build and test
        String url = buildAndTest(buildData, branch, sha, repoFullName, repoGitURI, gitPath);

        return MessageBuilder.withPayload(payload)
                .copyHeadersIfAbsent(msg.getHeaders())
                .setHeader("buildData", buildData)
                .setHeader("url", url)
                .build();
    }

    /**
     * Method that clones, builds and tests a gradlew repository.
     * @param buildData BuildData object stores information about the build
     * @param branch branch to checkout
     * @param sha commit to test
     * @param repoFullName full name of repository
     * @param repoGitURI URI of repository
     * @param gitPath clone destination folder
     * @return gradle scan url if available
     */
    private String buildAndTest(BuildData buildData, String branch, String sha, String repoFullName, String repoGitURI, File gitPath) {
        resetFolder(gitPath);
        buildData.setDateStart(new Date());
        ArrayList<String> buildLog = new ArrayList<>();
        String url = "";
        try {
            // Clone git repo branch
            addToBuildLog(buildLog, "git clone --branch=" + branch + " " + repoGitURI + " " + repoFullName);
            Git result = cloneGitRepo(branch, gitPath, repoGitURI);

            // hard reset to commit
            addToBuildLog(buildLog, "git reset --hard " + sha);
            result.reset().setMode(ResetCommand.ResetType.HARD).setRef(sha).call();

            // build with gradlew build --scan -s
            Process p = getGradleProcess(gitPath, buildLog);

            // Hijack input
            url = logGradlewOutput(buildLog, p);

            if (p.exitValue() == 0) {
                buildData.setBuildStatus(BuildStatus.success);
                buildData.setMessage("Build Success");
            }
            else {
                buildData.setBuildStatus(BuildStatus.failure);
                buildData.setMessage("Build Failure");
            }
        } catch (IOException|GitAPIException e) {
            logException(buildData, buildLog, e);
        } catch (InterruptedException e) {
            logException(buildData, buildLog, e);
            Thread.currentThread().interrupt();
        }
        buildData.setDateFinish(new Date());
        buildData.setBuildLog(buildLog);
        log2.info("Remove: " + gitPath);
        deleteDir(gitPath);
        log2.info("Exit build function");
        return url;
    }

    private void logException(BuildData buildData, ArrayList<String> buildLog, Exception e) {
        log2.error(e.getClass().getSimpleName(), e);
        buildLog.add(e.toString());
        buildData.setBuildStatus(BuildStatus.error);
        buildData.setMessage(e.getClass().getSimpleName() + ". See log");
    }

    /**
     * Hijacks the Gradlew output and prints it line by line to the buildLog
     * Also extracts the scan url if available, returns when gradlew is finished
     * @param buildLog build log
     * @param p gradlew process
     * @return gradle scan url if available
     * @throws IOException
     * @throws InterruptedException
     */
    private String logGradlewOutput(ArrayList<String> buildLog, Process p) throws IOException, InterruptedException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(p.getInputStream()) );
        String line;
        String urlExtract = "";
        // store input
        while ((line = in.readLine()) != null) {
            addToBuildLog(buildLog, line);
            if (line.startsWith("https://gradle.com/s/"))
                urlExtract = line;
        }
        in.close();
        p.waitFor();
        addToBuildLog(buildLog, "Exit: " + (p.exitValue() == 0));
        return urlExtract;
    }

    /**
     * Creates and runs gradlew in gitPath
     * @param gitPath gitPath folder where gradlew lives
     * @param buildLog build log progress
     * @return gradlew process object
     * @throws IOException problem with starting gradlew
     */
    private Process getGradleProcess(File gitPath, ArrayList<String> buildLog) throws IOException {
        final String buildCommand = gitPath.getCanonicalPath() + File.separator +
                "gradlew" +
                (System.getProperty("os.name").startsWith("Windows")?".bat":"");
        final String[] options = {buildCommand,"build","--scan","-s"};
        addToBuildLog(buildLog, "gradlew" +
                " " + options[1] +
                " " + options[2] +
                " " + options[3]);
        return new ProcessBuilder()
                .command(options)
                .directory(gitPath)
                .redirectErrorStream(true)
                .start();
    }

    /**
     * Add line to build log, also logs the line.
     * @param buildLog build log to augment
     * @param s line to add
     */
    private void addToBuildLog(ArrayList<String> buildLog, String s) {
        log2.info(s);
        buildLog.add(s);
    }

    /**
     * Reset folder by deleting the folder tree if it exists and recreate the root folder.
     * @param gitPath root folder
     */
    private void resetFolder(File gitPath) {
        log2.info("Create: " + gitPath);
        deleteDir(gitPath);
        gitPath.mkdir();
    }


    /**
     * Clones a Git repository onto disk and switches to a branch
     * Uses MY_PERSONAL_TOKEN env var if available for CredentialsProvider
     * @param branch branch to checkout
     * @param gitPath path to clone it
     * @param repoGitURI URI of .git repository
     * @return the results of the git clone
     * @throws GitAPIException on git api errors
     * @throws IOException on file system error
     */
    private static Git cloneGitRepo(String branch, File gitPath, String repoGitURI) throws GitAPIException, IOException {
        String token = System.getenv("MY_PERSONAL_TOKEN");
        if (token != null){
            CredentialsProvider cp = new UsernamePasswordCredentialsProvider(
                    "token",
                    System.getenv("MY_PERSONAL_TOKEN"));
            return cloneGitRepo(branch, gitPath, repoGitURI, cp);
        }
        return cloneGitRepo(branch, gitPath, repoGitURI, CredentialsProvider.getDefault());
    }

    /**
     * Clones a Git repository onto disk and switches to a branch
     * @param branch what branch to switch to
     * @param gitPath where to clone it
     * @param repoGitURI URI of .git repository
     * @param cp use this CredentialsProvider if not null
     * @return the results of the git clone
     * @throws GitAPIException on git api errors
     * @throws IOException on file system error
     */
    private static Git cloneGitRepo(String branch, File gitPath, String repoGitURI, CredentialsProvider cp) throws GitAPIException, IOException {
        CloneCommand command = Git.cloneRepository()
                .setURI(repoGitURI)
                .setDirectory(gitPath.getCanonicalFile())
                .setBranch(branch);
        if (cp != null)
            return command.setCredentialsProvider(cp).call();
        return command.call();
    }

    /**
     * Recursively remove files and folders in path. rm -r file
     * @param file file or folder to delete
     */
    private static void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (! Files.isSymbolicLink(f.toPath())) {
                    deleteDir(f);
                }
            }
        }
        file.delete();
    }
}
