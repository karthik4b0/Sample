import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;
import java.util.concurrent.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class BulkUploadServiceTest {

    @InjectMocks
    private BulkUploadService bulkUploadService;

    @Mock
    private UserEntitlementsService userEntitlementsService;

    @Mock
    private BulkUploadRepository bulkUploadRepository;

    @Mock
    private BulkUploadDocumentsRepository bulkUploadDocumentsRepository;

    @Mock
    private DocumentService documentService;

    @Mock
    private UtilsService utilsService;

    @Mock
    private Logger logger;

    private static final String BULK_UPLOAD_GUID = "test-guid";
    private static final String AUTH_USER_ID = "test-user-id";
    private static final String REQUEST_TRACKING_ID = "test-tracking-id";
    private static final int BATCH_SIZE = 40;
    private static final int SIMULTANEOUS_REQUESTS = 4;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testPublishBulkUploadSuccess() {
        // Mock user entitlements
        UserEntitlements mockUserEntitlements = new UserEntitlements();
        when(userEntitlementsService.getUserEntitlements()).thenReturn(mockUserEntitlements);

        // Mock bulk upload data
        BulkUpload mockBulkUpload = new BulkUpload();
        mockBulkUpload.setBulkUploadGUID(BULK_UPLOAD_GUID);
        when(bulkUploadRepository.findByBulkUploadGuid(anyString())).thenReturn(Optional.of(mockBulkUpload));

        // Mock the documents
        List<BulkUploadDocuments> mockDocuments = new ArrayList<>();
        BulkUploadDocuments mockDocument = new BulkUploadDocuments();
        mockDocument.setMetadataValidatedF1(true);
        mockDocument.setErrorF1(false);
        mockDocuments.add(mockDocument);

        when(bulkUploadDocumentsRepository.findAllByBulkUploadId(anyLong())).thenReturn(mockDocuments);

        // Mock document service saveAll
        doNothing().when(documentService).saveAll(anyList());

        // Mock utilsService.pushToUEAndUpdate
        doNothing().when(utilsService).pushToUEAndUpdate(anyString(), any(), anyList(), any(), any());

        // Mock save methods for BulkUpload and BulkUploadDocuments
        when(bulkUploadRepository.save(any(BulkUpload.class))).thenReturn(mockBulkUpload);
        when(bulkUploadDocumentsRepository.save(any(BulkUploadDocuments.class))).thenReturn(mockDocument);

        // Perform the test
        BulkUploadPublishResponseData responseData = bulkUploadService.publishBulkUpload(BULK_UPLOAD_GUID, AUTH_USER_ID, REQUEST_TRACKING_ID);

        // Assertions
        assertNotNull(responseData);
        assertEquals(BULK_UPLOAD_GUID, responseData.getData().getBulkUploadGuid());
        assertNotNull(responseData.getData().getBulkUploadDocuments());
        assertEquals(1, responseData.getData().getBulkUploadDocuments().size());

        // Verifying interactions
        verify(bulkUploadRepository, times(1)).save(any(BulkUpload.class));
        verify(bulkUploadDocumentsRepository, times(1)).save(any(BulkUploadDocuments.class));
    }

    @Test
    public void testPublishBulkUploadWithNoDocuments() {
        // Mock user entitlements
        UserEntitlements mockUserEntitlements = new UserEntitlements();
        when(userEntitlementsService.getUserEntitlements()).thenReturn(mockUserEntitlements);

        // Mock bulk upload data
        BulkUpload mockBulkUpload = new BulkUpload();
        mockBulkUpload.setBulkUploadGUID(BULK_UPLOAD_GUID);
        when(bulkUploadRepository.findByBulkUploadGuid(anyString())).thenReturn(Optional.of(mockBulkUpload));

        // Mock empty document list
        List<BulkUploadDocuments> mockDocuments = new ArrayList<>();
        when(bulkUploadDocumentsRepository.findAllByBulkUploadId(anyLong())).thenReturn(mockDocuments);

        // Perform the test
        BulkUploadPublishResponseData responseData = bulkUploadService.publishBulkUpload(BULK_UPLOAD_GUID, AUTH_USER_ID, REQUEST_TRACKING_ID);

        // Assertions
        assertNotNull(responseData);
        assertEquals(BULK_UPLOAD_GUID, responseData.getData().getBulkUploadGuid());
        assertNotNull(responseData.getData().getBulkUploadDocuments());
        assertEquals(0, responseData.getData().getBulkUploadDocuments().size());

        // Verifying that no documents were saved
        verify(bulkUploadDocumentsRepository, times(0)).save(any(BulkUploadDocuments.class));
    }

    @Test
    public void testPublishBulkUploadWithMultipleBatches() {
        // Mock user entitlements
        UserEntitlements mockUserEntitlements = new UserEntitlements();
        when(userEntitlementsService.getUserEntitlements()).thenReturn(mockUserEntitlements);

        // Mock bulk upload data
        BulkUpload mockBulkUpload = new BulkUpload();
        mockBulkUpload.setBulkUploadGUID(BULK_UPLOAD_GUID);
        when(bulkUploadRepository.findByBulkUploadGuid(anyString())).thenReturn(Optional.of(mockBulkUpload));

        // Mock the documents (more than batch size, e.g., 50 documents)
        List<BulkUploadDocuments> mockDocuments = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            BulkUploadDocuments mockDocument = new BulkUploadDocuments();
            mockDocument.setMetadataValidatedF1(true);
            mockDocument.setErrorF1(false);
            mockDocuments.add(mockDocument);
        }

        when(bulkUploadDocumentsRepository.findAllByBulkUploadId(anyLong())).thenReturn(mockDocuments);

        // Mock document service saveAll
        doNothing().when(documentService).saveAll(anyList());

        // Mock utilsService.pushToUEAndUpdate
        doNothing().when(utilsService).pushToUEAndUpdate(anyString(), any(), anyList(), any(), any());

        // Mock save methods for BulkUpload and BulkUploadDocuments
        when(bulkUploadRepository.save(any(BulkUpload.class))).thenReturn(mockBulkUpload);
        when(bulkUploadDocumentsRepository.save(any(BulkUploadDocuments.class))).thenReturn(mockDocuments.get(0));

        // Create a mock ExecutorService
        ExecutorService executorService = mock(ExecutorService.class);
        when(executorService.submit(any(Runnable.class))).thenReturn(mock(Future.class));

        bulkUploadService.executorService = executorService; // Inject mock ExecutorService

        // Perform the test
        BulkUploadPublishResponseData responseData = bulkUploadService.publishBulkUpload(BULK_UPLOAD_GUID, AUTH_USER_ID, REQUEST_TRACKING_ID);

        // Assertions
        assertNotNull(responseData);
        assertEquals(BULK_UPLOAD_GUID, responseData.getData().getBulkUploadGuid());
        assertNotNull(responseData.getData().getBulkUploadDocuments());
        assertEquals(50, responseData.getData().getBulkUploadDocuments().size());

        // Verifying ExecutorService interactions (ensuring tasks are submitted)
        verify(executorService, times(2)).submit(any(Runnable.class)); // 50 docs, 2 batches (assume batchSize = 40)
    }

    @Test
    public void testExecutorServiceShutdown() throws InterruptedException {
        // Mock ExecutorService to verify shutdown
        ExecutorService executorService = mock(ExecutorService.class);
        when(executorService.awaitTermination(60, TimeUnit.SECONDS)).thenReturn(true);
        bulkUploadService.executorService = executorService;

        // Simulate the publish logic
        bulkUploadService.publishBulkUpload(BULK_UPLOAD_GUID, AUTH_USER_ID, REQUEST_TRACKING_ID);

        // Verify that the executor service is shut down correctly
        verify(executorService, times(1)).shutdown();
        verify(executorService, times(1)).awaitTermination(60, TimeUnit.SECONDS);
    }
}
