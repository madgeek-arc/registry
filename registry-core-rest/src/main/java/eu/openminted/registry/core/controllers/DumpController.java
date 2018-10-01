package eu.openminted.registry.core.controllers;

import eu.openminted.registry.core.service.DumpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
public class DumpController {

    @Autowired
    DumpService dumpService;

    static boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (null != files) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        deleteDirectory(files[i]);
                    } else {
                        files[i].delete();
                    }
                }
            }
        }
        return (directory.delete());
    }



    @RequestMapping(value = "/dump/", method = RequestMethod.GET)
    @ResponseBody
//    @PreAuthorize("hasRole('ROLE_USER')")
    public void dumpAll(
                        @RequestParam(value="raw", required = false, defaultValue = "false") String raw,
                        @RequestParam(value = "schema", required = false, defaultValue = "false") String schema,
                        @RequestParam(value = "version", required = false, defaultValue = "false") String version,
                        @RequestParam(value = "resourceTypes", required = false, defaultValue = "") String[] resourceTypes,
                        HttpServletRequest request,
                        HttpServletResponse response ) {

        ServletContext context = request.getServletContext();
        String appPath = context.getRealPath("");
        System.out.println("appPath = " + appPath);

        // construct the complete absolute path of the file
        boolean wantVersion = false;
        if(version.equals("true"))
            wantVersion=true;

        boolean isRaw = false;
        if(raw.equals("true"))
            isRaw=true;

        boolean wantSchema = false;
        if(schema.equals("true"))
            wantSchema = true;


        File downloadFile = null;
        downloadFile = dumpService.bringAll(isRaw, wantSchema, resourceTypes, wantVersion);

        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(downloadFile);
        } catch (FileNotFoundException e) {
            deleteDirectory(downloadFile);
            e.printStackTrace();
            return;
        }

        // get MIME type of the file
        String mimeType = context.getMimeType(downloadFile.getAbsolutePath());
        if (mimeType == null) {
            // set to binary type if MIME mapping not found
            mimeType = "application/octet-stream";
        }
        System.out.println("MIME type: " + mimeType);

        // set content attributes for the response
        response.setContentType(mimeType);
        response.setContentLength((int) downloadFile.length());

        // set headers for the response
        String headerKey = "Content-Disposition";
        SimpleDateFormat sdfDate = new SimpleDateFormat("ddMMyyyy");//dd/MM/yyyy
        Date now = new Date();
        String strDate = sdfDate.format(now);
        String headerValue = String.format("attachment; filename=\"dump-%s.zip\"",
                strDate);
        response.setHeader(headerKey, headerValue);

        // get output stream of the response
        OutputStream outStream;
        try {
            outStream = response.getOutputStream();
        } catch (IOException e) {
            System.out.println(downloadFile);
            deleteDirectory(downloadFile);
            e.printStackTrace();
            return;
        }

        byte[] buffer = new byte[4096];
        int bytesRead = -1;

        // write bytes read from the input stream into the output stream
        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            inputStream.close();
            outStream.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println(downloadFile);
        deleteDirectory(downloadFile);
    }
}
