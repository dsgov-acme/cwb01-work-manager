package io.nuvalence.workmanager.service.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordExpirationServiceTest {
    private RecordExpirationService recordExpirationService;

    @Mock private RecordService recordService;

    @BeforeEach
    void setUp() {
        recordExpirationService = new RecordExpirationService(recordService);
    }

    @Test
    void expireRecordsTest() {
        int expectedExpiredRecords = 5;
        when(recordService.expireRecords()).thenReturn(expectedExpiredRecords);

        recordExpirationService.expireRecords();

        verify(recordService, times(1)).expireRecords();
    }
}
