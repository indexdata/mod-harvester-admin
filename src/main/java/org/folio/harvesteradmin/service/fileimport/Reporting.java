package org.folio.harvesteradmin.service.fileimport;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.harvesteradmin.moduledata.Entity;
import org.folio.harvesteradmin.moduledata.LogLine_;
import org.folio.harvesteradmin.moduledata.database.ModuleStorageAccess;
import org.folio.harvesteradmin.utils.SettableClock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Reporting {

    private final long startTime;
    private final AtomicInteger filesProcessed = new AtomicInteger(0);
    private final AtomicInteger recordsProcessed = new AtomicInteger(0);
    private final InventoryMetrics inventoryMetrics = new InventoryMetrics();
    private final BlockingQueue<FileStats> fileStats = new ArrayBlockingQueue<>(2);
    private final ModuleStorageAccess storage;
    private final ImportJob importJob;

    public static final Logger logger = LogManager.getLogger("reporting");


    public Reporting(ImportJob handler, String tenant, Vertx vertx) {
        this.importJob = handler;
        this.startTime = System.currentTimeMillis();
        this.storage = new ModuleStorageAccess(vertx, tenant);
    }

    public void nowProcessing(String fileName) {
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

    /**
     * Reports at end-of-current file
     */
    public void endOfFile() {
        incrementFilesProcessed();
        reportFileStats();
    }

    public void endOfQueue() {
        reportFileQueueStats(true);
    }

    public void reportFileStats() {
        try {
            if (!fileStats.isEmpty()) {
                FileStats stats = fileStats.peek();
                assert stats != null;
                log("File #" + filesProcessed.get() + " (" + stats.getFileName() + ") "
                        + stats.getRecordsProcessed() + " records in " + processingTimeAsString(stats.processingTime()) + " (" + (stats.getRecordsProcessed() * 1000L / stats.processingTime()) +
                        " recs/s.)")
                        .compose(na -> log(stats.getInventoryMetrics().report()));
                fileStats.take();
            } else {
                logger.info("reportFileStats(): FileStatus queue was empty");
            }
        } catch (InterruptedException ie) {
            logger.error("Error reporting file statistics: " + ie.getMessage());
        }
    }

    public void reportFileQueueStats(boolean queueDone) {
        long processingTime = (System.currentTimeMillis() - startTime);
        log((queueDone ? "Done processing queue. " : "") + filesProcessed + " file(s) with " + recordsProcessed.get() +
                " records processed in " + processingTimeAsString(processingTime) + " (" +
                (recordsProcessed.get() * 1000L / processingTime) + " recs/s.)")
                .compose(na -> queueDone ? log(inventoryMetrics.report()) : null);
        if (queueDone) {
            importJob.setFinishedDateTime();

            logger.info("Done processing queue. " + filesProcessed + " file(s) with " + recordsProcessed.get() +
                    " records processed in " + processingTimeAsString(processingTime) + " (" +
                    (recordsProcessed.get() * 1000L / processingTime) + " recs/s.)");
        }
    }

    private static String processingTimeAsString (long processingTime) {
        int hours = (int) processingTime/(1000*60*60);
        long remainingMs = processingTime % (1000*60*60);
        int minutes = (int) remainingMs/(1000*60);
        remainingMs = remainingMs % (1000*60);
        int seconds = (int) remainingMs/1000;
        return (hours>0 ? hours + " hours " : "") +  (hours>0 || minutes>0 ? minutes  + " minutes " : "") + seconds + " seconds";
    }

    private Future<Void> log (String statement) {
        List<Entity> lines = new ArrayList<>();
        lines.add(new LogLine_(
                UUID.randomUUID(),
                importJob.importJobLog.record.id(),
                SettableClock.getLocalDateTime().toString(),
                importJob.importJobLog.record.importConfigName(),
                statement));
        return storage.storeLogLines_(lines);
    }
}
