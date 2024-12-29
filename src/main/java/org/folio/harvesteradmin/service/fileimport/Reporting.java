package org.folio.harvesteradmin.service.fileimport;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Reporting {

    public final AtomicBoolean idleQueue = new AtomicBoolean(true);
    private final AtomicLong startTime = new AtomicLong();
    private final AtomicInteger filesProcessed = new AtomicInteger(0);
    private final AtomicInteger recordsProcessed = new AtomicInteger(0);
    private final BlockingQueue<FileStats> fileStats = new ArrayBlockingQueue<>(2);
    private final String jobId;
    private final FileQueue fileQueue;

    public Reporting (FileQueue fileQueue) {
        this.jobId = fileQueue.getJobId();
        this.fileQueue = fileQueue;
    }

    public void nextFileProcessing(String fileName) {
        if (idleQueue.get()) {
            //System.out.println("nextFileProcessing() queue marked idle, resetting records, files processed, star time ");
            recordsProcessed.set(0);
            filesProcessed.set(0);
            startTime.set(System.currentTimeMillis());
            idleQueue.set(false);
        }
        try {
            //System.out.println("Put FileStats for " + fileName + " in queue.");
            fileStats.put(new FileStats(fileName));
        } catch (InterruptedException ignore) {}
        //System.out.println(fileStats.size() + " FileStats in queue.");
    }

    public boolean pendingFileStats() {
        return !fileStats.isEmpty();
    }

    public void incrementRecordsProcessed(int delta) {
        recordsProcessed.addAndGet(delta);
        if (fileStats.peek()!=null) fileStats.peek().incrementRecordsProcessed(delta);
    }

    public void reportFileQueueStats() {
        long processingTime = (System.currentTimeMillis() - startTime.get());
        System.out.println((fileQueueDone()? "Done processing queue for job " : "Job ") + jobId + ": " + filesProcessed + " file(s) with " + recordsProcessed.get() +
                " records processed in " + getProcessingTimeAsString(processingTime) + " (" +
                (recordsProcessed.get() * 1000L / processingTime) + " recs/s.)");
    }

    public boolean fileQueueDone() {
        if (!fileQueue.hasNextFile() && !pendingFileStats()) {
            idleQueue.set(true);
            return true;
        }
        return false;
    }

    public void incrementFilesProcessed() {
        filesProcessed.incrementAndGet();
    }

    public void reportFileStats() {
        try {
            if (!fileStats.isEmpty()) {
                FileStats stats = fileStats.peek();
                assert stats != null;
                System.out.println("File #" + filesProcessed.get() + " (" + stats.getFileName() + ") "
                        + stats.getRecordsProcessed() + " records in " + getProcessingTimeAsString(stats.processingTime()) + " (" + (stats.getRecordsProcessed() * 1000L / stats.processingTime()) +
                        " recs/s.). Job ID " + jobId + ".");
                fileStats.take();
            } else {
                System.out.println("reportFileStats(): FileStatus queue was empty");
            }
        } catch (InterruptedException ie) { System.out.println(ie.getMessage());}
    }

    public String getProcessingTimeAsString (long processingTime) {
        int hours = (int) processingTime/(1000*60*60);
        long remainingMs = processingTime % (1000*60*60);
        int minutes = (int) remainingMs/(1000*60);
        remainingMs = remainingMs % (1000*60);
        int seconds = (int) remainingMs/1000;
        return (hours>0 ? hours + " hours " : "") +  (hours>0 || minutes>0 ? minutes  + " minutes " : "") + seconds + " seconds";
    }

}
