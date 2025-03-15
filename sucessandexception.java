import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.*;

@SpringBootTest
public class BulkUploadServiceTest {

    @Mock
    private BulkUploadDocumentsRepository bulkUploadDocumentsRepository;

    @Mock
    private BulkUploadRepository bulkUploadRepository;

    @Mock
    private DocumentService documentService;

    @Mock
    private UtilsService utilsService;

    @Mock
    private Logger logger;

    @InjectMocks
    private BulkUploadService bulkUploadService;

    @Mock
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testPublishBulkUploadSuccess() throws InterruptedException, ExecutionException {
        // Prepare mock data
        String bulkUploadGuid = "testGuid";
        String authUserId = "authUser";
        String requestTrackingId = "trackingId";

        BulkUpload aBulkUpload = new BulkUpload();
        aBulkUpload.setBulkUploadId(1L);
        aBulkUpload.setBulkUploadGUID(bulkUploadGuid);
        when(bulkUploadRepository.save(any())).thenReturn(aBulkUpload);

        BulkUploadDocuments bulkUploadDocument = new BulkUploadDocuments();
        bulkUploadDocument.setBulkUploadId(1L);
        bulkUploadDocument.setMetadataValidatedF1(true);
        bulkUploadDocument.setErrorF1(false);
        bulkUploadDocument.setPublishFl(false);
        bulkUploadDocument.setDocument(new Document());

        List<BulkUploadDocuments> documentsList = new ArrayList<>();
        documentsList.add(bulkUploadDocument);

        when(bulkUploadDocumentsRepository.findAllByBulkUploadId(anyLong())).thenReturn(documentsList);
        when(documentService.saveAll(anyList())).thenReturn(documentsList);

        // Mocking executor service
        ExecutorService executorService = mock(ExecutorService.class);
        when(executorService.submit(any(Runnable.class))).thenReturn(mock(Future.class));

        // Call the method under test
        BulkUploadPublishResponseData responseData = bulkUploadService.publishBulkUpload(bulkUploadGuid, authUserId, requestTrackingId);

        // Assertions
        assertNotNull(responseData);
        assertEquals(bulkUploadGuid, responseData.getData().getBulkUploadGuid());
        verify(bulkUploadRepository, times(1)).save(any());
        verify(bulkUploadDocumentsRepository, times(1)).saveAll(any());
        verify(executorService, times(1)).submit(any(Runnable.class));
    }

    @Test
    void testPublishBulkUploadWithEmptyMetaDataValidatedDocuments() {
        // Prepare mock data
        String bulkUploadGuid = "testGuid";
        String authUserId = "authUser";
        String requestTrackingId = "trackingId";

        BulkUpload aBulkUpload = new BulkUpload();
        aBulkUpload.setBulkUploadId(1L);
        aBulkUpload.setBulkUploadGUID(bulkUploadGuid);
        when(bulkUploadRepository.save(any())).thenReturn(aBulkUpload);

        List<BulkUploadDocuments> emptyList = new ArrayList<>();
        when(bulkUploadDocumentsRepository.findAllByBulkUploadId(anyLong())).thenReturn(emptyList);

        // Call the method under test
        BulkUploadPublishResponseData responseData = bulkUploadService.publishBulkUpload(bulkUploadGuid, authUserId, requestTrackingId);

        // Assertions
        assertNotNull(responseData);
        assertTrue(responseData.getData().getBulkUploadDocuments().isEmpty());
        verify(bulkUploadRepository, times(1)).save(any());
        verify(bulkUploadDocumentsRepository, times(0)).saveAll(any());
    }

    @Test
    void testExecutorServiceShutdown() throws InterruptedException {
        // Simulate the documents to be processed in multiple batches
        List<Document> docList = new ArrayList<>();
        docList.add(new Document());
        docList.add(new Document());
        docList.add(new Document());

        List<List<Document>> docPartitions = ListUtils.partition(docList, 2);
        ExecutorService executorService = mock(ExecutorService.class);
        when(executorService.submit(any(Runnable.class))).thenReturn(mock(Future.class));

        // Call publishBulkUpload with mock data
        String bulkUploadGuid = "testGuid";
        String authUserId = "authUser";
        String requestTrackingId = "trackingId";
        BulkUploadPublishResponseData responseData = bulkUploadService.publishBulkUpload(bulkUploadGuid, authUserId, requestTrackingId);

        // Ensure that shutdown was called after submitting tasks
        verify(executorService, times(1)).shutdown();
        verify(executorService, times(1)).awaitTermination(anyLong(), any(TimeUnit.class));
    }

    @Test
    void testPublishBulkUploadWithExceptionHandling() throws InterruptedException {
        // Simulate exception during document push to UE
        List<Document> docList = new ArrayList<>();
        docList.add(new Document());

        List<List<Document>> docPartitions = ListUtils.partition(docList, 1);
        ExecutorService executorService = mock(ExecutorService.class);

        // Mock the execution of push task to throw exception
        doThrow(new DocException("Error pushing to UE")).when(executorService).submit(any(Runnable.class));

        // Call the method under test
        String bulkUploadGuid = "testGuid";
        String authUserId = "authUser";
        String requestTrackingId = "trackingId";
        BulkUploadPublishResponseData responseData = bulkUploadService.publishBulkUpload(bulkUploadGuid, authUserId, requestTrackingId);

        // Verify that the exception handling was triggered
        verify(executorService, times(1)).submit(any(Runnable.class));
    }
}
