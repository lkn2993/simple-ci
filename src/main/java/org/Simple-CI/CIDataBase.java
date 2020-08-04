package org.Simple-CI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.info.ProjectInfoProperties;

import java.io.*;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
/**
 * Database for buildhistory. Saves build data objects and retrieves build data objects.
 */
public class CIDataBase {

    private final Logger log = LoggerFactory.getLogger(CIDataBase.class);
    ArrayList<BuildData> allBuilds;
    File currentfolder;

    /*

    @param String is a hash that uniquely describes a commit, date is the date that commit was made.
    @return returns the json object for that commit.
     */

    public CIDataBase(String folderName){
        ArrayList<BuildData> allBuildsList = new ArrayList<BuildData>();
        if(!folderName.isEmpty()){
            currentfolder = new File(folderName);
        } else {
            currentfolder = new File("./database");
        }
        if(!currentfolder.isDirectory()){
            currentfolder.mkdir();
        }
        File[] allFiles = currentfolder.listFiles();
        for (File jsonFile:
             allFiles) {
            try{
                ArrayList<BuildData> arr = readBuild(jsonFile);
                for (int i = 0; i < arr.size() ; i++){
                    allBuildsList.add(arr.get(i));
                }
            } catch(IOException e){
                log.error(e.toString());
            }
        }
        allBuilds = allBuildsList;
    }
    /*
    @param a hash for a commit
    @return all builds that was done on a commit with the sha = hash
     */
    public ArrayList<BuildData> readAllBuild(String hash) {
        ArrayList<BuildData> arr;
        arr = new ArrayList<BuildData>();
        for (int i = 0; i < allBuilds.size();i++){
            if(allBuilds.get(i).getSha().equals(hash)){
                arr.add(allBuilds.get(i));
            }
        }
        return arr;
    }
    /*
    @param amount of build Datas to return
    @return returns an arraylist with the amount of build datas requested
     */
    public ArrayList<BuildData> getAllBuilds(int amount){
        if(amount>allBuilds.size()){
            return allBuilds;
        }
        return new ArrayList<BuildData>(allBuilds.subList(0,amount));
    }
    /*
    @return returns all of our builds
     */
    public ArrayList<BuildData> getAllBuilds(){
        return allBuilds;
    }
    /*

    @param hash is the name of the file to be read.
    @return returns the BuildData that was put in that file
     */
    public ArrayList<BuildData> readBuild(File file) throws IOException{
        BufferedReader br;
        synchronized(this) {
            try {
                br = new BufferedReader(new FileReader(file));
            } catch (FileNotFoundException e) {
                log.error(e.toString());
                return null;
            }
            ArrayList<BuildData> answer = new ArrayList<BuildData>();
            while (true) {
                BuildData json;
                try {
                    json = new BuildData(br.readLine(), new Date(Long.parseLong(br.readLine())), br.readLine());
                } catch (Exception e) {
                    br.close();
                    return answer;
                }
                json.setDateStart(new Date(Long.parseLong(br.readLine())));
                json.setDateFinish(new Date(Long.parseLong(br.readLine())));
                int i = Integer.parseInt(br.readLine());
                ArrayList<String> buildLogs = new ArrayList<String>(i);
                for (int j = 0; j < i; j++) {
                    buildLogs.add(br.readLine());
                }
                json.setBuildLog(buildLogs);
                String str = br.readLine();
                if (str.equals("error")) {
                    json.setBuildStatus(BuildStatus.error);
                } else if (str.equals("failure")) {
                    json.setBuildStatus(BuildStatus.failure);
                } else if (str.equals("pending")) {
                    json.setBuildStatus(BuildStatus.pending);
                } else if (str.equals("success")) {
                    json.setBuildStatus(BuildStatus.success);
                }
                json.setMessage(br.readLine());
                answer.add(json);
            }
        }
    }
    /*
    @param the hash for the given build and the date for the specific build with that hash.
    @return the build in BuildData form.
     */
    public ArrayList<BuildData> readAllBuild(String hash,Date date) throws IOException{
        ArrayList<BuildData> bd = readAllBuild(hash);
        ArrayList<BuildData> answer = new ArrayList<BuildData>();
        for (BuildData data:
             bd) {
            if(data.getDateStart()==date){
                answer.add(data);
            }
        }
        return answer;
    }
    /*

    @param a data object that wants to be written to the database.
    @return true if it succeeded and false if it failed.
     */
    public boolean writeBuild(BuildData json) throws IOException{
        BufferedWriter bw;
        // Only one thread at the time can write to the database
        synchronized (this) {
            try {
                bw = new BufferedWriter(new FileWriter(new File(currentfolder, json.getSha()), true));
            } catch (FileNotFoundException e) {
                log.error(e.toString());
                return false;
            }
            bw.write(json.getRepoFullName());
            bw.write('\n');
            bw.write("" + json.getDateCreated().getTime());
            bw.write('\n');
            bw.write(json.getSha());
            bw.write('\n');
            bw.write("" + json.getDateStart().getTime());
            bw.write('\n');
            bw.write("" + json.getDateFinish().getTime());
            bw.write('\n');
            ArrayList<String> buildLogs = (ArrayList<String>) json.getBuildLog();
            bw.write("" + buildLogs.size());
            bw.write('\n');
            for (int i = 0; i < buildLogs.size(); i++) {
                bw.write(buildLogs.get(i));
                bw.write('\n');
            }
            bw.write(json.getBuildStatus().name());
            bw.write('\n');
            bw.write(json.getMessage());
            bw.write('\n');
            bw.flush();
            allBuilds.add(json);
            bw.close();
        }
        return true;
    }
    /*
    @param String specifying what hash to remove
    @return the list of buildDatas that were removed
     */
    public ArrayList<BuildData> removeBuildData(String hash) throws IOException{
        ArrayList<BuildData> bds = readAllBuild(hash);
        allBuilds.removeAll(bds);
        synchronized (this) {
            File f = new File(currentfolder.getAbsoluteFile() + File.separator + hash);
            f.delete();
        }
        return bds;
    }
}
