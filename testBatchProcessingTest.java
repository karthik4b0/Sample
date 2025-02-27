package com.seic.docmgmt.dql.services.v2;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BulkUploadPublishServiceTest {

    @Mock
    private UtilsService utilsService;

    @InjectMocks
    private BulkUploadPublishService bulkUploadPublishService;

    @Mock
    private Logger logger;

    private List<Document> docList;
    private String requestTrackingId = "tracking123";
    private String userEntitlements = "testEntitlement";
    private int batchSize = 5;
    private int simultaneousRequests = 3;

    @BeforeEach
    public void setup() {
        // Prepare the mock document list
        docList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Document doc = new Document();
            doc.setDocumentId((long) i);
            docList.add(doc);
        }
    }

    @Test
    public void testBatchProcessingAndExecutorService() throws InterruptedException {
        // Mock ExecutorService and UtilsService
        ExecutorService executorService = mock(ExecutorService.class);
        doNothing().when(executorService).submit(any(Runnable.class)); // Mock submit method
        doNothing().when(executorService).shutdown();
        when(executorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Call the method to test the logic
        bulkUploadPublishService.processDocumentsInBatches(docList, batchSize, simultaneousRequests, requestTrackingId, userEntitlements);

        // Verify that the documents were partitioned into 2 batches (since 10 docs and batch size is 5)
        verify(executorService, times(2)).submit(any(Runnable.class));  // We expect 2 tasks to be submitted

        // Verify that the executor was shut down correctly
        verify(executorService).shutdown();
        verify(executorService).awaitTermination(60, TimeUnit.SECONDS);
    }

    @Test
    public void testPushToUEAndUpdateCalledForEachBatch() throws InterruptedException {
        // Mock ExecutorService
        ExecutorService executorService = mock(ExecutorService.class);
        doNothing().when(executorService).submit(any(Runnable.class)); // Mock submit method
        doNothing().when(executorService).shutdown();
        when(executorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Mock the actual call to the pushToUEAndUpdate method
        doNothing().when(utilsService).pushToUEAndUpdate(anyString(), anyString(), anyList(), any(), any());

        // Call the method to test the logic
        bulkUploadPublishService.processDocumentsInBatches(docList, batchSize, simultaneousRequests, requestTrackingId, userEntitlements);

        // Verify that pushToUEAndUpdate was called for each document batch (2 batches)
        verify(utilsService, times(2)).pushToUEAndUpdate(anyString(), anyString(), anyList(), any(), any());

        // Verify that the shutdown and termination methods are still called
        verify(executorService).shutdown();
        verify(executorService).awaitTermination(60, TimeUnit.SECONDS);
    }

    @Test
    public void testGracefulShutdownOfExecutorService() throws InterruptedException {
        // Mock ExecutorService
        ExecutorService executorService = mock(ExecutorService.class);
        doNothing().when(executorService).submit(any(Runnable.class)); // Mock submit method
        doNothing().when(executorService).shutdown();
        when(executorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Call the method to test the logic
        bulkUploadPublishService.processDocumentsInBatches(docList, batchSize, simultaneousRequests, requestTrackingId, userEntitlements);

        // Verify that shutdown and awaitTermination were called to gracefully shut down the executor
        verify(executorService).shutdown();
        verify(executorService).awaitTermination(60, TimeUnit.SECONDS);
    }

    @Test
    public void testInterruptedExceptionDuringExecution() throws InterruptedException {
        // Mock ExecutorService and UtilsService
        ExecutorService executorService = mock(ExecutorService.class);
        doNothing().when(executorService).submit(any(Runnable.class)); // Mock submit method
        doNothing().when(executorService).shutdown();
        when(executorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException());

        // Call the method to test the logic
        bulkUploadPublishService.processDocumentsInBatches(docList, batchSize, simultaneousRequests, requestTrackingId, userEntitlements);

        // Verify that the executor was shut down even if there was an InterruptedException
        verify(executorService).shutdown();
        verify(executorService).awaitTermination(60, TimeUnit.SECONDS);
    }
}
