import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.concurrent.*;

@SpringBootTest
public class BulkUploadServiceTest {

    @Mock
    private BulkUploadRepository bulkUploadRepository;

    @Mock
    private BulkUploadDocumentsRepository bulkUploadDocumentsRepository;

    @Mock
    private DocumentService documentService;

    @Mock
    private UtilsService utilsService;

    @Mock
    private ExecutorService executorService;

    @InjectMocks
    private BulkUploadPublishService bulkUploadPublishService;

    @Captor
    private ArgumentCaptor<Runnable> taskCaptor;

    private String bulkUploadGuid = "bulkUploadGuid";
    private String authUserId = "authUserId";
    private String requestTrackingId = "requestTrackingId";
    private int batchSize = 5; // assuming this value
    private int simultaneousRequests = 8; // assuming this value

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock executor service to simulate the behavior of submitting tasks
        when(executorService.submit(any(Runnable.class))).thenReturn(null);
    }

    @Test
    public void testPublishPushToUESuccess() {
        // Setup mock data for bulk upload and documents
        BulkUpload bulkUpload = new BulkUpload();
        bulkUpload.setBulkUploadId(1L);
        bulkUpload.setBulkUploadGUID(bulkUploadGuid);
        bulkUpload.setPublishFl(false); // not published yet

        List<BulkUploadDocuments> allDocuments = new ArrayList<>();
        BulkUploadDocuments doc1 = new BulkUploadDocuments();
        doc1.setMetadataValidatedF1(true);
        doc1.setErrorF1(false);
        doc1.setDocument(new Document()); // Mock document

        BulkUploadDocuments doc2 = new BulkUploadDocuments();
        doc2.setMetadataValidatedF1(true);
        doc2.setErrorF1(false);
        doc2.setDocument(new Document()); // Mock document

        allDocuments.add(doc1);
        allDocuments.add(doc2);

        when(bulkUploadRepository.findByBulkUploadGUID(bulkUploadGuid)).thenReturn(bulkUpload);
        when(bulkUploadDocumentsRepository.findAllByBulkUploadId(anyLong())).thenReturn(allDocuments);
        when(documentService.saveAll(anyList())).thenReturn(new ArrayList<>()); // Mock save all
        when(bulkUploadDocumentsRepository.saveAll(anyList())).thenReturn(allDocuments);

        // Mock the method pushToUEAndUpdate to simulate success
        doNothing().when(utilsService).pushToUEAndUpdate(eq(requestTrackingId), any(), anyList(), any(), any());

        try {
            // Call the method under test
            BulkUploadPublishResponseData result = bulkUploadPublishService.publishBulkUpload(bulkUploadGuid, authUserId, requestTrackingId);

            // Assertions to check that everything went as expected
            assertNotNull(result);
            assertNotNull(result.getData());
            assertEquals(bulkUploadGuid, result.getData().getBulkUploadGuid());

            // Verify that tasks are submitted to the executor
            verify(executorService, atLeastOnce()).submit(taskCaptor.capture());

            // Capture the submitted task and check if it executes correctly
            Runnable submittedTask = taskCaptor.getValue();
            assertNotNull(submittedTask);

            // Execute the captured task (without starting new threads)
            submittedTask.run();

            // Verify that pushToUEAndUpdate was called
            verify(utilsService, atLeastOnce()).pushToUEAndUpdate(eq(requestTrackingId), any(), anyList(), any(), any());

            // Verify repository interactions
            verify(bulkUploadRepository, times(1)).save(bulkUpload);
            verify(bulkUploadDocumentsRepository, atLeastOnce()).saveAll(anyList());
            verify(documentService, atLeastOnce()).saveAll(anyList());
        } catch (Exception e) {
            fail("Exception during test execution: " + e.getMessage());
        }
    }
}
