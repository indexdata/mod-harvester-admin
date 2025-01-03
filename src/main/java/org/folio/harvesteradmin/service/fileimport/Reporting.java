package org.folio.harvesteradmin.service.fileimport;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Reporting {

    private final String jobId;
    private final AtomicLong startTime = new AtomicLong();
    private final AtomicInteger filesProcessed = new AtomicInteger(0);
    private final AtomicInteger recordsProcessed = new AtomicInteger(0);
    private final InventoryMetrics inventoryMetrics = new InventoryMetrics();
    private final BlockingQueue<FileStats> fileStats = new ArrayBlockingQueue<>(2);

    public Reporting (JobHandler handler) {
        this.jobId = handler.getJobId();
    }

    public void nowProcessing(String fileName, boolean resetCounters) {
        if (resetCounters) {
            recordsProcessed.set(0);
            filesProcessed.set(0);
            startTime.set(System.currentTimeMillis());
        }
        try {
            fileStats.put(new FileStats(fileName));
        } catch (InterruptedException ignore) {}
    }

    public void incrementFilesProcessed() {
        filesProcessed.incrementAndGet();
    }

    public void incrementInventoryMetrics(InventoryMetrics metrics) {
        inventoryMetrics.add(metrics);
        if (fileStats.peek()!=null) fileStats.peek().addInventoryMetrics(metrics);
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
                System.out.println(stats.getInventoryMetrics().report());
                fileStats.take();
            } else {
                System.out.println("reportFileStats(): FileStatus queue was empty");
            }
        } catch (InterruptedException ie) { System.out.println(ie.getMessage());}
    }

    public void reportFileQueueStats(boolean queueDone) {
        long processingTime = (System.currentTimeMillis() - startTime.get());
        System.out.println((queueDone ? "Done processing queue for job " : "Job ") + jobId + ": " + filesProcessed + " file(s) with " + recordsProcessed.get() +
                " records processed in " + processingTimeAsString(processingTime) + " (" +
                (recordsProcessed.get() * 1000L / processingTime) + " recs/s.)");
        System.out.println(inventoryMetrics.report());

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
