package io.nuvalence.workmanager.service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service that expires records.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecordExpirationService {

    private final RecordService recordService;

    @Scheduled(cron = "0 0 0 * * *", zone = "America/New_York")
    @SchedulerLock(name = "RecordExpirationJob", lockAtMostFor = "30m", lockAtLeastFor = "5m")
    public void expireRecords() {
        int recordsExpired = recordService.expireRecords();
        log.info("Expired {} records", recordsExpired);
    }
}
