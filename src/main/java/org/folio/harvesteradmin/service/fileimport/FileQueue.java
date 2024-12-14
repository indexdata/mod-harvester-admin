package org.folio.harvesteradmin.service.fileimport;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;


public class FileQueue {

    public static final String SOURCE_FILES_ROOT_DIR = "source-files";
    public static final String HARVEST_JOB_FILE_PROCESSING_DIR = "processing";
    private final String tenantRootDir;
    private final String processingDirectoryName;
    private final FileSystem fs;

    public FileQueue(Vertx vertx, String tenant) {
        this.fs = vertx.fileSystem();
        String sourceFilesRootDir = SOURCE_FILES_ROOT_DIR;
        this.tenantRootDir = sourceFilesRootDir + "/" + tenant;
        this.processingDirectoryName = HARVEST_JOB_FILE_PROCESSING_DIR;
        if (!fs.existsBlocking(sourceFilesRootDir)) {
            fs.mkdirBlocking(sourceFilesRootDir);
        }
        if (!fs.existsBlocking(tenantRootDir)) {
            fs.mkdirBlocking(tenantRootDir);
        }
    }

    /**
     * Creates a new file in the staging directory for the given job configuration.
     * @param jobId ID of the job configuration and thus the name of the job's staging directory.
     * @param fileName The name of the file to stage.
     * @param file The file contents.
     */
    public void addNewFile(String jobId, String fileName, Buffer file) {
        String jobPath = pathToJobFiles(jobId);
        if (! fs.existsBlocking(jobPath)) {
            fs.mkdirsBlocking(jobPath + "/" + processingDirectoryName).mkdirBlocking(jobPath + "/tmp");
        }
        fs.writeFileBlocking(jobPath + "/tmp/" + fileName, file)
                .moveBlocking(jobPath+"/tmp/"+fileName, jobPath+"/"+fileName);
    }

    /**
     * Checks if there is a file in the processing directory for the
     * given job ID or if it's empty and thus available for the next file to harvest.
     * @param jobId ID of a job configuration and thus the name of the job's staging directory.
     * @return true if the processing directory is empty and thus ready for the next file, otherwise false.
     */
    public boolean couldPromoteNextFile(String jobId) {
        return fs.readDirBlocking(pathToProcessingSlot(jobId)).isEmpty();
    }

    public boolean hasNextFile(String jobId) {
        Optional<File> nextFile = fs.readDirBlocking(pathToJobFiles(jobId)).stream().map(File::new)
                .filter(File::isFile).min(Comparator.comparing(File::lastModified));
        return nextFile.isPresent();
    }
    /**
     * Promotes the next file in the staging directory to the processing directory
     * and returns true if a staged file was found, otherwise returns false.
     * @param jobId ID of a job configuration and thus the name of the job's staging directory.
     * @return true if another file was found for processing, otherwise false.
     */
    public boolean promoteNextFile(String jobId) {
        Optional<File> nextFile = fs.readDirBlocking(pathToJobFiles(jobId)).stream().map(File::new)
                .filter(File::isFile).min(Comparator.comparing(File::lastModified));
        if (nextFile.isPresent()) {
            fs.moveBlocking(nextFile.get().getPath(), pathToProcessingSlot(jobId) + "/" + nextFile.get().getName());
            return true;
        }
        return false;
    }

    private String pathToProcessingSlot(String jobId) {
        return pathToJobFiles(jobId) + "/" + processingDirectoryName;
    }

    private String pathToJobFiles(String jobId) {
        return tenantRootDir + "/" + jobId;
    }

    /**
     * Gets the name of the file currently processing under the given job configuration.
     * @param jobId ID of a job configuration and thus the name of the job's staging directory.
     * @return The name of file being processed, "none" if there is none.
     */
    public String currentlyPromotedFile(String jobId) {
        return couldPromoteNextFile(jobId) ? "none" : fs.readDirBlocking(pathToProcessingSlot(jobId)).get(0);
    }

    public void deleteFile(File file) {
        fs.deleteBlocking(file.getPath());
    }

    /**
     * Get a list of the names of the job directories currently created in the file system.
     * @return list of IDs
     */
    public List<String> getJobIds() {
        return fs.readDirBlocking(tenantRootDir)
                .stream().filter(p -> fs.propsBlocking(p).isDirectory()).map(p -> new File(p).getName())
                .collect(Collectors.toList());
    }

}
