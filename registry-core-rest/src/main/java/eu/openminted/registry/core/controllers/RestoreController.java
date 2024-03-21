package eu.openminted.registry.core.controllers;

import eu.openminted.registry.core.domain.BatchResult;
import eu.openminted.registry.core.service.RestoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
public class RestoreController {

    @Autowired
    RestoreService restoreService;


    @RequestMapping(value = "/restore/", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, BatchResult> restoreAll(@RequestParam("datafile") MultipartFile file) {
        return restoreService.restoreDataFromZip(file);
    }

}
