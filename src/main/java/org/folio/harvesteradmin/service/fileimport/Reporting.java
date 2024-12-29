package org.folio.harvesteradmin.service.fileimport;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Reporting {

    private final String jobId;
    private final FileQueue fileQueue;
    public final AtomicBoolean betweenQueuesOfFiles = new AtomicBoolean(true);
    private final AtomicLong startTime = new AtomicLong();
    private final AtomicInteger filesProcessed = new AtomicInteger(0);
    private final AtomicInteger recordsProcessed = new AtomicInteger(0);
    private final BlockingQueue<FileStats> fileStats = new ArrayBlockingQueue<>(2);

    public Reporting (FileQueue fileQueue) {
        this.jobId = fileQueue.getJobId();
        this.fileQueue = fileQueue;
    }

    public void markNextFileProcessing(String fileName) {
        if (betweenQueuesOfFiles.get()) {
            //System.out.println("nextFileProcessing() queue marked idle, resetting records, files processed, star time ");
            recordsProcessed.set(0);
            filesProcessed.set(0);
            startTime.set(System.currentTimeMillis());
            betweenQueuesOfFiles.set(false);
        }
        try {
            //System.out.println("Put FileStats for " + fileName + " in queue.");
            fileStats.put(new FileStats(fileName));
        } catch (InterruptedException ignore) {}
        //System.out.println(fileStats.size() + " FileStats in queue.");
    }

    public void incrementFilesProcessed() {
        filesProcessed.incrementAndGet();
    }

    public boolean pendingFileStats() {
        return !fileStats.isEmpty();
    }

    public void incrementRecordsProcessed(int delta) {
        recordsProcessed.addAndGet(delta);
        if (fileStats.peek()!=null) fileStats.peek().incrementRecordsProcessed(delta);
    }

    public void reportFileStats() {
        try {
            if (!fileStats.isEmpty()) {
                FileStats stats = fileStats.peek();
                assert stats != null;
                System.out.println("Job " + jobId + ": File #" + filesProcessed.get() + " (" + stats.getFileName() + ") "
                        + stats.getRecordsProcessed() + " records in " + processingTimeAsString(stats.processingTime()) + " (" + (stats.getRecordsProcessed() * 1000L / stats.processingTime()) +
                        " recs/s.)");
                fileStats.take();
            } else {
                System.out.println("reportFileStats(): FileStatus queue was empty");
            }
        } catch (InterruptedException ie) { System.out.println(ie.getMessage());}
    }

    public boolean fileQueueDone() {
        if (!fileQueue.hasNextFile() && !pendingFileStats()) {
            betweenQueuesOfFiles.set(true);
            return true;
        }
        return false;
    }

    public void reportFileQueueStats() {
        long processingTime = (System.currentTimeMillis() - startTime.get());
        System.out.println((fileQueueDone()? "Done processing queue for job " : "Job ") + jobId + ": " + filesProcessed + " file(s) with " + recordsProcessed.get() +
                " records processed in " + processingTimeAsString(processingTime) + " (" +
                (recordsProcessed.get() * 1000L / processingTime) + " recs/s.)");
    }

    private static String processingTimeAsString (long processingTime) {
        int hours = (int) processingTime/(1000*60*60);
        long remainingMs = processingTime % (1000*60*60);
        int minutes = (int) remainingMs/(1000*60);
        remainingMs = remainingMs % (1000*60);
        int seconds = (int) remainingMs/1000;
        return (hours>0 ? hours + " hours " : "") +  (hours>0 || minutes>0 ? minutes  + " minutes " : "") + seconds + " seconds";
    }

}
