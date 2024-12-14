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
    private final String jobPath;
    private final String pathToProcessingSlot;
    private final FileSystem fs;

    public FileQueue(Vertx vertx, String tenant, String jobId) {
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
        jobPath = tenantRootDir + "/" + jobId;
        pathToProcessingSlot = jobPath + "/" + processingDirectoryName;
        if (! fs.existsBlocking(jobPath)) {
            fs.mkdirsBlocking(pathToProcessingSlot).mkdirBlocking(jobPath + "/tmp");
        }
    }

    /**
     * Creates a new file in the staging directory for the given job configuration.
     * @param fileName The name of the file to stage.
     * @param file The file contents.
     */
    public void addNewFile(String fileName, Buffer file) {
        fs.writeFileBlocking(jobPath + "/tmp/" + fileName, file)
                .moveBlocking(jobPath+"/tmp/"+fileName, jobPath+"/"+fileName);
    }

    /**
     * Checks if there is a file in the processing directory for the
     * given job ID or if it's empty and thus available for the next file to harvest.
     * @param jobId ID of a job configuration and thus the name of the job's staging directory.
     * @return true if the processing directory is empty and thus ready for the next file, otherwise false.
     */
    public boolean couldPromoteNextFile() {
        return fs.readDirBlocking(pathToProcessingSlot).isEmpty();
    }

    public boolean hasNextFile() {
        Optional<File> nextFile = fs.readDirBlocking(jobPath).stream().map(File::new)
                .filter(File::isFile).min(Comparator.comparing(File::lastModified));
        return nextFile.isPresent();
    }
    /**
     * Promotes the next file in the staging directory to the processing directory
     * and returns true if a staged file was found, otherwise returns false.
     * @param jobId ID of a job configuration and thus the name of the job's staging directory.
     * @return true if another file was found for processing, otherwise false.
     */
    public boolean promoteNextFile() {
        Optional<File> nextFile = fs.readDirBlocking(jobPath).stream().map(File::new)
                .filter(File::isFile).min(Comparator.comparing(File::lastModified));
        if (nextFile.isPresent()) {
            fs.moveBlocking(nextFile.get().getPath(), pathToProcessingSlot + "/" + nextFile.get().getName());
            return true;
        }
        return false;
    }

    /**
     * Gets the name of the file currently processing under the given job configuration.
     * @return The name of file being processed, "none" if there is none.
     */
    public String currentlyPromotedFile() {
        return couldPromoteNextFile() ? "none" : fs.readDirBlocking(pathToProcessingSlot).get(0);
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
