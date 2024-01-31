package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.record.Record;
import io.nuvalence.workmanager.service.domain.record.RecordDefinition;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.generated.models.RecordUpdateRequest;
import io.nuvalence.workmanager.service.mapper.EntityMapper;
import io.nuvalence.workmanager.service.mapper.MissingSchemaException;
import io.nuvalence.workmanager.service.models.RecordFilters;
import io.nuvalence.workmanager.service.repository.RecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class RecordServiceTest {

    @Mock private RecordRepository recordRepository;

    @Mock private RecordFactory recordFactory;

    @Mock private EntityMapper entityMapper;

    @Mock private ApplicationContext applicationContext;
    @Mock private TransactionService transactionService;

    @InjectMocks private RecordService recordService;

    private Clock clock = Clock.systemDefaultZone();

    private EntityMapper mapper;

    @BeforeEach
    void setUp() {
        openMocks(this);
        clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

        ObjectMapper objectMapper = new ObjectMapper();
        mapper = Mappers.getMapper(EntityMapper.class);
        mapper.setApplicationContext(applicationContext);
        mapper.setObjectMapper(objectMapper);
    }

    @Test
    void testCreateRecord() throws MissingSchemaException {
        // Mock data
        RecordDefinition recordDefinition = new RecordDefinition();
        Transaction transaction = new Transaction();
        Record record = new Record();

        // Mock behavior
        when(recordFactory.createRecord(any(RecordDefinition.class), any(Transaction.class)))
                .thenReturn(record);
        when(recordRepository.save(any(Record.class))).thenReturn(record);

        // Perform the test
        Record result = recordService.createRecord(recordDefinition, transaction);

        // Verify the interactions and assertions
        verify(recordFactory, times(1)).createRecord(recordDefinition, transaction);
        verify(recordRepository, times(1)).save(record);
        assertEquals(record, result);
    }

    @Test
    void testGetRecordById() {
        // Mock data
        UUID id = UUID.randomUUID();
        Record record = new Record();

        // Mock behavior
        when(recordRepository.findById(id)).thenReturn(Optional.of(record));

        // Perform the test
        Optional<Record> result = recordService.getRecordById(id);

        // Verify the interactions and assertions
        verify(recordRepository, times(1)).findById(id);
        assertEquals(Optional.of(record), result);
    }

    @Test
    void testGetRecordsByFilters() {
        // Mock data
        final RecordFilters filters =
                new RecordFilters(null, null, "externalId", "createdTimestamp", "ASC", 0, 2);
        Page<Record> recordPage = mock(Page.class);

        // Mock behavior
        when(recordRepository.findAll(any(), (Pageable) any())).thenReturn(recordPage);

        // Perform the test
        Page<Record> result = recordService.getRecordsByFilters(filters);

        // Verify the interactions and assertions
        verify(recordRepository, times(1)).findAll(any(), (Pageable) any());
        assertEquals(recordPage, result);
    }

    @Test
    void testUpdateRecord() throws MissingSchemaException {
        // Mock data
        RecordUpdateRequest updateRequest = new RecordUpdateRequest();
        updateRequest.setStatus("NEW_STATUS");
        updateRequest.setExpires(OffsetDateTime.now(clock));
        Map<String, Object> dataRequested = Map.of("key", "value");
        updateRequest.setData(dataRequested);

        Schema schema = Schema.builder().name("Address").property("key", String.class).build();

        Record existingRecord = new Record();
        existingRecord.setStatus("OLD_STATUS");
        existingRecord.setExpires(OffsetDateTime.now(clock).minusMinutes(3));
        existingRecord.setData(new DynamicEntity(Schema.builder().build()));
        DynamicEntity test = mapper.convertGenericMapToEntity(schema, dataRequested);

        // Mock behavior
        when(recordRepository.save(any(Record.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionService.unifyAttributeMaps(any(), any())).thenReturn(dataRequested);
        when(entityMapper.convertAttributesToGenericMap(any())).thenReturn(dataRequested);
        when(entityMapper.convertGenericMapToEntity(any(), any())).thenReturn(test);

        // Perform the test
        Record result = recordService.updateRecord(updateRequest, existingRecord);

        // Verify the interactions and assertions
        verify(recordRepository, times(1)).save(existingRecord);
        assertEquals("NEW_STATUS", result.getStatus());
        assertEquals(OffsetDateTime.now(clock), result.getExpires());
        assertEquals("key", result.getData().getDynaClass().getDynaProperty("key").getName());
        assertEquals("value", result.getData().get("key"));
    }

    @Test
    void testHasAdminDataChanges() {
        // Mock data
        Record originalRecord = new Record();
        originalRecord.setStatus("OLD_STATUS");
        originalRecord.setExpires(OffsetDateTime.now(clock).minusMinutes(3));

        // Test case 1: Status has changed
        assertTrue(recordService.hasAdminDataChanges(originalRecord, "NEW_STATUS", null));

        // Test case 2: Expires has changed
        assertTrue(
                recordService.hasAdminDataChanges(
                        originalRecord, null, OffsetDateTime.now(clock).toString()));

        // Test case 3: Both status and expires have changed
        assertTrue(
                recordService.hasAdminDataChanges(
                        originalRecord, "NEW_STATUS", OffsetDateTime.now(clock).toString()));

        // Test case 4: No changes
        assertFalse(recordService.hasAdminDataChanges(originalRecord, null, null));
    }

    @Test
    void testExpireRecords() {
        int expectedExpiredRecordsCount = 5;
        when(recordRepository.updateStatusForExpiredRecords())
                .thenReturn(expectedExpiredRecordsCount);

        recordService.expireRecords();
        verify(recordRepository, times(1)).updateStatusForExpiredRecords();
    }
}
