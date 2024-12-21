package org.folio.harvesteradmin.service.fileimport;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Reporting {

    private final AtomicBoolean idleQueue = new AtomicBoolean(true);
    private long startTime;
    private final AtomicInteger filesProcessed = new AtomicInteger(0);
    private final AtomicInteger recordsProcessed = new AtomicInteger(0);
    private final BlockingQueue<FileStats> fileStats = new ArrayBlockingQueue<>(2);

    public void nextFileProcessing(String fileName) {
        if (idleQueue.get()) {
            recordsProcessed.set(0);
            filesProcessed.set(0);
            startTime = System.currentTimeMillis();
            idleQueue.set(false);
        }
        try {
            fileStats.put(new FileStats(fileName));
        } catch (InterruptedException ignore) {}
    }

    public void incrementRecordsProcessed(int delta) {
        recordsProcessed.addAndGet(delta);
        if (fileStats.peek()!=null) fileStats.peek().incrementRecordsProcessed(delta);
    }

    public void reportFileQueueStats() {
        long processingTime = (System.currentTimeMillis() - startTime);
        System.out.println(filesProcessed + " file(s) with " + recordsProcessed.get() +
                " records processed in " + getProcessingTimeAsString(processingTime) + " (" +
                (recordsProcessed.get() * 1000L / processingTime) + " recs/s.)");
        idleQueue.set(true);
    }

    public void incrementFilesProcessed() {
        filesProcessed.incrementAndGet();
    }

    public void reportFileStats() {
        try {
            if (!fileStats.isEmpty()) {
                FileStats stats = fileStats.take();
                System.out.println("File #" + filesProcessed.get() + " (" + stats.getFileName() + ") "
                        + stats.getRecordsProcessed() + " records in " + getProcessingTimeAsString(stats.processingTime()) + " (" + (stats.getRecordsProcessed() * 1000L / stats.processingTime()) +
                        " recs/s.)");
            }
        } catch (InterruptedException ignore) { System.out.println(ignore.getMessage());}
    }

    public String getProcessingTimeAsString (long processingTime) {
        int hours = (int) processingTime/(1000*60*60);
        long remainingMs = processingTime % (1000*60*60);
        int minutes = (int) remainingMs/(1000*60);
        remainingMs = remainingMs % (1000*60);
        int seconds = (int) remainingMs/1000;
        return (hours>0 ? hours + " hours " : "") +  (hours>0 && minutes>0 ? minutes  + " minutes " : "") + seconds + " seconds";
    }

}
