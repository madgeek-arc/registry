/**
 * Copyright 2018-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.registry.controllers;

import gr.uoa.di.madgik.registry.service.DumpService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
public class DumpController {

    private final DumpService dumpService;

    public DumpController(DumpService dumpService) {
        this.dumpService = dumpService;
    }

    private static boolean deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (null != files) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        Files.delete(file.toPath());
                    }
                }
            }
        }
        return Files.deleteIfExists(directory.toPath());
    }


    @RequestMapping(value = "/dump/", method = RequestMethod.GET)
    @ResponseBody
    public void dumpAll(
            @RequestParam(value = "raw", required = false, defaultValue = "false") String raw,
            @RequestParam(value = "schema", required = false, defaultValue = "false") String schema,
            @RequestParam(value = "version", required = false, defaultValue = "false") String version,
            @RequestParam(value = "resourceTypes", required = false, defaultValue = "") String[] resourceTypes,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        ServletContext context = request.getServletContext();
        String appPath = context.getRealPath("");

        // construct the complete absolute path of the file
        boolean wantVersion = false;
        if (version.equals("true"))
            wantVersion = true;

        boolean isRaw = false;
        if (raw.equals("true"))
            isRaw = true;

        boolean wantSchema = false;
        if (schema.equals("true"))
            wantSchema = true;

        File downloadFile = null;
        downloadFile = dumpService.dump(isRaw, wantSchema, resourceTypes, wantVersion);

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

        FileInputStream inputStream;
        // get output stream of the response
        OutputStream outStream;
        try {
            inputStream = new FileInputStream(downloadFile);
            outStream = response.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead = -1;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outStream.close();
        } catch (IOException e) {
            throw new ServiceException(e.getMessage(), e);
        } finally {
            deleteDirectory(downloadFile);
        }
    }
}
