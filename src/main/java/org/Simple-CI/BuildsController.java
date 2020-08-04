package org.Simple-CI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


import java.util.ArrayList;
import java.util.Date;

/**
 * This is a spring controller class responsible for handling MVC url decision making
 * For this project we have used spring MVC to implement a webserver
 * This class specifies what happens when a client connects to a specific url
 */
@Controller
public class BuildsController {

    @Value("${spring.application.name}")
    String appName;

    private static final int buildsPerPage = 50;
    /**
     * This function is responsible for handling requests sent to /builds url of the webserver
     */
    @GetMapping("/builds")
    public String builds(@RequestParam(name="page", required=false, defaultValue="1") String page,
            @RequestParam(name="sha", required=false, defaultValue="") String sha, Model model) {
        ArrayList<BuildData> buildsToGet;
        if (!sha.isEmpty())
            buildsToGet = CiApplication.CIDB.readAllBuild(sha);
        else
            buildsToGet = CiApplication.CIDB.getAllBuilds();
        int count = buildsToGet.size();
        int pageNum = Integer.parseInt(page);
        int startIndex;
        int finalIndex;
        if (pageNum * buildsPerPage < count)
        {
            startIndex = (pageNum - 1) * buildsPerPage;
            finalIndex = (pageNum * buildsPerPage) + 1;
        }
        else
        {
            finalIndex = count;
            if (count - 50 < 0)
                startIndex = 0;
            else
                startIndex = count - 50;
        }
        ArrayList<BuildData> buildsToShow;
        buildsToShow = new ArrayList<>(buildsToGet.subList(startIndex, finalIndex));
        //buildsToShow.add(new BuildData("Example", new Date()
        //        ,"0000000000000000000000000000000000000000"));
        model.addAttribute("page", pageNum);
        model.addAttribute("sha", sha);
        model.addAttribute("appName", appName);
        model.addAttribute("buildsToShow", buildsToShow);
        return "builds";
    }

}
