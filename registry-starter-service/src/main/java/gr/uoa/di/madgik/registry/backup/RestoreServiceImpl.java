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

package gr.uoa.di.madgik.registry.backup;

import gr.uoa.di.madgik.registry.backup.restore.RestoreJobListener;
import gr.uoa.di.madgik.registry.domain.BatchResult;
import gr.uoa.di.madgik.registry.service.RestoreService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


@Service("restoreService")
public class RestoreServiceImpl implements RestoreService {

    private static final Logger logger = LoggerFactory.getLogger(RestoreServiceImpl.class);

    private final JobLauncher jobLauncher;
    private final Job restoreJob;

    public RestoreServiceImpl(JobLauncher jobLauncher,
                              @Qualifier("restoreJob") Job restoreJob) {
        this.jobLauncher = jobLauncher;
        this.restoreJob = restoreJob;
    }

    File unzipFile(MultipartFile file) throws IOException {
        File zip = File.createTempFile(UUID.randomUUID().toString(), "temp");
        FileOutputStream o = new FileOutputStream(zip);
        IOUtils.copy(file.getInputStream(), o);
        o.close();

        UnzipUtility unzipUtility = new UnzipUtility();
        Path tempDirPath = Files.createTempDirectory("decompress");
        File tempDirFile = tempDirPath.toFile();
        unzipUtility.unzip(zip.getAbsolutePath(), tempDirPath.toString());
        zip.deleteOnExit();
        return tempDirFile;
    }

    @Override
    public Map<String, BatchResult> restoreDataFromZip(MultipartFile zipFile) {
        /**
         * save file to temp
         */
        Map<String, BatchResult> ret;
        try {
            File tempDirFile = unzipFile(zipFile);
            Optional<File[]> f = Optional.ofNullable(tempDirFile.listFiles());
            File[] resourceTypeFiles = f.orElse(new File[]{});
            Date date = new Date();
            RestoreJobListener restoreJobListener = new RestoreJobListener();
            for (File file : resourceTypeFiles) {
                JobParametersBuilder builder = new JobParametersBuilder();
                builder.addString("resourceType", file.getName());
                builder.addString("resourceTypeDir", file.getAbsolutePath());
                builder.addDate("date", date);
                ((AbstractJob) restoreJob).registerJobExecutionListener(restoreJobListener);
                JobExecution job = jobLauncher.run(restoreJob, builder.toJobParameters());
                restoreJobListener.registerJob(job);
            }
            logger.info("{}", restoreJobListener.waitResults());
            ret = restoreJobListener.getJobs().stream().map(this::convertJob).collect(Collectors.toMap(BatchResult::getResourceType, Function.identity()));

            // clean up temp folder
            FileUtils.cleanDirectory(tempDirFile);
            if (!tempDirFile.delete()) {
                logger.warn("Could not delete temporary folder '{}'", tempDirFile.getAbsolutePath());
            }
        } catch (Exception e) {
            throw new ServiceException("Failed to restore data from zip", e);
        }

        return ret;
    }

    private BatchResult convertJob(JobExecution j) {
        BatchResult ret = new BatchResult();
        Optional<Throwable> e = j.getAllFailureExceptions().stream().reduce(Throwable::initCause);
        e.ifPresent(throwable -> logger.warn(throwable.getMessage(), throwable));
        List<StepExecution> steps = new ArrayList<>(j.getStepExecutions());
        ret.setDroped(steps.get(0).getExitStatus().equals(ExitStatus.NOOP));
        ret.setStatus(j.getStatus().name());
        ret.setReadCount(steps.get(1).getReadCount());
        ret.setReadSkipCount(steps.get(1).getReadSkipCount());
        ret.setWriteCount(steps.get(1).getWriteCount());
        ret.setWriteSkipCount(steps.get(1).getWriteSkipCount());
        ret.setResourceType(j.getJobParameters().getString("resourceType"));
        return ret;
    }

    public class UnzipUtility {
        /**
         * Size of the buffer to read/write data
         */
        private static final int BUFFER_SIZE = 4096;

        /**
         * Extracts a zip file specified by the zipFilePath to a directory specified by
         * destDirectory (will be created if does not exists)
         *
         * @param zipFilePath
         * @param destDirectory
         * @throws IOException
         */
        public void unzip(String zipFilePath, String destDirectory) throws IOException {
            File destDir = new File(destDirectory);
            if (!destDir.exists()) {
                destDir.mkdir();
            }
            ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
            ZipEntry entry = zipIn.getNextEntry();
            // iterates over entries in the zip file
            while (entry != null) {
                String filePath = destDirectory + File.separator + entry.getName();
                boolean isDir = false;

                String[] splitInto = entry.getName().split("/");

                File tmpFile = null;
                if (splitInto.length < 2) {//it's a dir
                    tmpFile = new File(destDirectory + File.separator + splitInto[0]);
                    isDir = true;
                } else if (splitInto.length == 2) {//no versions included
                    tmpFile = new File(destDirectory + File.separator + splitInto[splitInto.length - 2]);
                } else {//versions included
                    tmpFile = new File(destDirectory + File.separator + splitInto[splitInto.length - 3] + File.separator + splitInto[splitInto.length - 2]);
                }

                if (!tmpFile.exists())
                    tmpFile.mkdirs();

                if (!isDir)
                    extractFile(zipIn, filePath);

                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
            zipIn.close();
        }

        /**
         * Extracts a zip entry (file entry)
         *
         * @param zipIn
         * @param filePath
         * @throws IOException
         */
        private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
            byte[] bytesIn = new byte[BUFFER_SIZE];
            int read = 0;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
            bos.close();
        }


    }


}
