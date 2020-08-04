package org.Simple-CI;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Test suite for {@link CIDataBase}
 */
public class CIDataBaseTest {

    private final String repoFullName = "xmas92/test-project";
    private final String sha = "75bb896e1bf493c700d29fd5c0ffeae84682d6ab";
    private final Date dateCreated = new Date(1549380803);
    private final String message = "message";
    /*
    trying the easiest database, add one element and then remove it.
     */
    @Test
    public void DatabaseBuildTest() {
        CIDataBase cdb = new CIDataBase("");
        BuildData buildData = new BuildData(repoFullName,dateCreated,sha);
        buildData.setDateStart(dateCreated);
        buildData.setDateFinish(dateCreated);
        List<String> buildLog = new ArrayList<>(Arrays.asList(new String[]{
                "Line1",
                "Line2",
                "Line3"
        }));
        buildData.setBuildStatus(BuildStatus.error);
        buildData.setMessage("Hejsan");
        buildData.setBuildLog(buildLog);
        try{
            cdb.writeBuild(buildData);
            ArrayList<BuildData> bd = cdb.readAllBuild(sha);
            assertEquals(buildData,bd.get(0));
            cdb.removeBuildData(sha);
        } catch (IOException e){
            System.out.println(e);
            assert false;
        }
    }
    /*
    Test that an empty hash gives an empty arraylist
     */
    @Test
    public void DatabaseBuildTestsEmpty() {
        CIDataBase cdb = new CIDataBase("");
        assertEquals(cdb.readAllBuild("").size(),0);
    }
    /*
    Test that an empty hash gives an empty arraylist
     */
    @Test
    public void DatabaseBuildTestsTwo() {
        CIDataBase cdb = new CIDataBase("");
        BuildData buildData = new BuildData(repoFullName,dateCreated,"75bb896e1bf493c700d29fd5c0ffeae84682d6ab");
        buildData.setDateStart(dateCreated);
        buildData.setDateFinish(dateCreated);
        List<String> buildLog = new ArrayList<>(Arrays.asList(new String[]{
                "Line1",
                "Line2",
                "Line3"
        }));
        buildData.setBuildStatus(BuildStatus.error);
        buildData.setMessage("Hejsan");
        buildData.setBuildLog(buildLog);
        BuildData buildData2 = new BuildData(repoFullName,dateCreated,"75bb896e1bf493c700d29fd5c0ffeae84682d6ad");
        buildData2.setDateStart(dateCreated);
        buildData2.setDateFinish(dateCreated);
        buildData2.setBuildStatus(BuildStatus.error);
        buildData2.setMessage("Hejsan");
        buildData2.setBuildLog(buildLog);
        try{
            cdb.writeBuild(buildData);
            cdb.writeBuild(buildData2);
            assertEquals(cdb.readAllBuild("75bb896e1bf493c700d29fd5c0ffeae84682d6ad").get(0),buildData2);
            assertEquals(cdb.readAllBuild("75bb896e1bf493c700d29fd5c0ffeae84682d6ab").get(0),buildData);
            cdb.removeBuildData(sha);
            cdb.removeBuildData("75bb896e1bf493c700d29fd5c0ffeae84682d6ad");
        } catch (Exception e){
            assert false;
        }
    }
    /*
    Test that opening another database reads the same data from the file.
     */
    @Test
    public void DatabaseBuildTestsTwoDatabases() {
        CIDataBase cdb = new CIDataBase("");
        BuildData buildData = new BuildData(repoFullName,dateCreated,sha);
        buildData.setDateStart(dateCreated);
        buildData.setDateFinish(dateCreated);
        List<String> buildLog = new ArrayList<>(Arrays.asList(new String[]{
                "Line1",
                "Line2",
                "Line3"
        }));
        buildData.setBuildStatus(BuildStatus.error);
        buildData.setMessage("Hejsan");
        buildData.setBuildLog(buildLog);
        try{
            cdb.writeBuild(buildData);
            CIDataBase cdb2 = new CIDataBase("");
            assertEquals(cdb2.readAllBuild(sha).get(0),cdb2.readAllBuild(sha).get(0));
        } catch (Exception e){
            assert false;
        }
    }
    /*
    Tests the get all builds.
    */
    @Test
    public void DataBaseTestGetAllBuilds() {
        CIDataBase cdb = new CIDataBase("");
        cdb.getAllBuilds();
        cdb.getAllBuilds(2);
    }
    /*
    Tests get all builds with one build
     */
    @Test
    public void DatabaseBuildGetAllBuildOne() {
        CIDataBase cdb = new CIDataBase("");
        BuildData buildData = new BuildData(repoFullName,dateCreated,sha);
        buildData.setDateStart(dateCreated);
        buildData.setDateFinish(dateCreated);
        List<String> buildLog = new ArrayList<>(Arrays.asList(new String[]{
                "Line1",
                "Line2",
                "Line3"
        }));
        buildData.setBuildStatus(BuildStatus.error);
        buildData.setMessage("Hejsan");
        buildData.setBuildLog(buildLog);
        try{
            cdb.writeBuild(buildData);
            assertEquals(cdb.getAllBuilds(4).get(0),buildData);
            assertEquals(cdb.getAllBuilds().get(0),buildData);
        } catch (Exception e){
            assert false;
        }
    }
}
