package org.folio.harvesteradmin.service.fileimport;

import java.util.concurrent.atomic.AtomicInteger;

public class FileStats {
    private final String fileName;
    private final long startTime;
    private final AtomicInteger recordsProcessed = new AtomicInteger(0);

    public FileStats(String fileName) {
        this.fileName = fileName;
        startTime = System.currentTimeMillis();
    }

    public void incrementRecordsProcessed(int delta) {
        recordsProcessed.addAndGet(delta);
    }

    public long processingTime() {
        return System.currentTimeMillis()-startTime;
    }

    public String getFileName () {
        return fileName;
    }

    public int getRecordsProcessed () {
        return recordsProcessed.get();
    }

    public String toString () {
        return "Stats for " + fileName + ". Start time " + startTime;
    }
}
