package org.folio.harvesteradmin.service.fileimport;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FileQueueProcessingReport {

    private long startTime;
    private final AtomicInteger filesProcessed = new AtomicInteger(0);
    private final AtomicInteger recordsProcessed = new AtomicInteger(0);
    private final AtomicBoolean queueEmpty = new AtomicBoolean(false);
    private final AtomicBoolean lastRecordProcessed = new AtomicBoolean(false);
    private String currentFileName;
    private long fileStartTime;
    private final AtomicInteger recordsInFile = new AtomicInteger(0);

    public FileQueueProcessingReport(FileQueue fileQueue) {
    }

    public void nextFileProcessing(String fileName) {
        if (filesProcessed.incrementAndGet() == 1) {
            lastRecordProcessed.set(false);
            queueEmpty.set(false);
            startTime = System.currentTimeMillis();
        }
        fileStartTime = System.currentTimeMillis();
        recordsInFile.set(0);
        currentFileName = fileName;
    }

    public void incrementRecordsProcessed () {
        recordsProcessed.incrementAndGet();
        recordsInFile.incrementAndGet();
    }

    public void lastRecordProcessed() {
        lastRecordProcessed.set(true);
    }

    public void endOfFileQueue() {
        long processingTime = (System.currentTimeMillis() - startTime);
        System.out.println("Reporting: " + filesProcessed + " file(s) with " + recordsProcessed.get() +
                " records processed in " + getProcessingTimeAsString(processingTime) + " seconds ~" +
                (recordsProcessed.get() / (processingTime/1000)) + " records per second.");
        queueEmpty.set(true);
    }

    public void reportFileProcessed () {
        long fileProcessingTime = System.currentTimeMillis()-fileStartTime;
        System.out.println("Reporting: File #" +filesProcessed.get() + ", " + currentFileName + ", " + recordsInFile + " records, processed in  " + getProcessingTimeAsString(fileProcessingTime) + ".");
    }


    public String getProcessingTimeAsString (long processingTime) {
        int hours = (int) processingTime/(1000*60*60);
        long remainingMs = processingTime % (1000*60*60);
        int minutes = (int) remainingMs/(1000*60);
        remainingMs = remainingMs % (1000*60);
        int seconds = (int) remainingMs/1000;
        return (hours>0 ? hours + " hours " : "") +  minutes + " minutes " + seconds + " seconds";
    }

}
