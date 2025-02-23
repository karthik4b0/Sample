import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BulkUploadPublishServiceTest {

    @Captor
    ArgumentCaptor<String> captor;

    @InjectMocks
    private BulkUploadPublishService bulkUploadPublishService;

    @Mock
    private UtilsService utilsService; // Assuming you have UtilsService that handles pushToUEAndUpdate
    @Mock
    private ExecutorService executorService; // Mocking the ExecutorService
    @Mock
    private BulkUploadRepository bulkUploadRepository;
    @Mock
    private BulkUploadDocumentsRepository bulkUploadDocumentsRepository;
    @Mock
    private DocumentService documentService;
    @Mock
    private BulkUploadErrorRepository bulkUploadErrorRepository;

    private String bulkUploadGuid = UUID.randomUUID().toString();
    private String requestTrackingId = UUID.randomUUID().toString();
    private String authUserId = "svcDocUser";

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testPublishPushTOUESuccess() throws Exception {
        // Setup mock data
        BulkUpload bulkUpload = buildBulkUpload();
        when(bulkUploadRepository.findByBulkUploadGUID(anyString())).thenReturn(bulkUpload);

        List<BulkUploadDocuments> bulkUploadDocuments = buildBulkUploadDocuments();
        when(bulkUploadDocumentsRepository.findAllByBulkUploadId(anyLong())).thenReturn(bulkUploadDocuments);
        when(documentService.saveAll(anyList())).thenReturn(new ArrayList<>());
        when(bulkUploadDocumentsRepository.saveAll(anyList())).thenReturn(bulkUploadDocuments);
        when(bulkUploadRepository.save(any())).thenReturn(bulkUpload);

        // Mock the ExecutorService's behavior (avoiding actual threading)
        Future<?> mockFuture = mock(Future.class);
        when(executorService.submit(any(Runnable.class))).thenReturn(mockFuture);

        // Call the method under test
        BulkUploadPublishResponseData response = bulkUploadPublishService.publishBulkUpload(
            bulkUploadGuid, authUserId, bulkUploadGuid, requestTrackingId
        );

        // Assertions to check the response
        assertNotNull(response);
        assertNotNull(response.getData());
        assertNotNull(response.getData().getBulkUploadDocuments());

        assertEquals(bulkUploadGuid, response.getData().getBulkUploadGuid());
        assertEquals(2, response.getData().getBulkUploadDocuments().size());

        // Check document properties
        assertEquals("doc1", response.getData().getBulkUploadDocuments().get(0).getDocumentName());
        assertEquals("doc2", response.getData().getBulkUploadDocuments().get(1).getDocumentName());

        // Verify interactions with the mocked executor
        verify(executorService, times(1)).submit(any(Runnable.class));
        verify(utilsService, times(2)).pushToUEAndUpdate(any(), any(), any(), any(), any());
    }

    @Test
    public void testPublishPushToUEException() throws Exception {
        // Setup mock data
        BulkUpload bulkUpload = buildBulkUpload();
        when(bulkUploadRepository.findByBulkUploadGUID(anyString())).thenReturn(bulkUpload);

        List<BulkUploadDocuments> bulkUploadDocuments = buildBulkUploadDocuments();
        when(bulkUploadDocumentsRepository.findAllByBulkUploadId(anyLong())).thenReturn(bulkUploadDocuments);
        when(documentService.saveAll(anyList())).thenReturn(new ArrayList<>());
        when(bulkUploadDocumentsRepository.saveAll(anyList())).thenReturn(bulkUploadDocuments);
        when(bulkUploadRepository.save(any())).thenReturn(bulkUpload);

        // Mock the ExecutorService's behavior (avoiding actual threading)
        Future<?> mockFuture = mock(Future.class);
        when(executorService.submit(any(Runnable.class))).thenReturn(mockFuture);

        // Simulate an exception in pushToUEAndUpdate
        doThrow(new DocException("Error pushing to UE")).when(utilsService).pushToUEAndUpdate(any(), any(), any(), any(), any());

        try {
            bulkUploadPublishService.publishBulkUpload(bulkUploadGuid, authUserId, bulkUploadGuid, requestTrackingId);
            fail("Expected DocException to be thrown");
        } catch (DocException e) {
            // Assert that the exception is thrown correctly
            assertEquals("Error pushing to UE", e.getMessage());
        }

        // Verify that the exception was thrown and handle the interactions
        verify(executorService, times(1)).submit(any(Runnable.class));
        verify(utilsService, times(2)).pushToUEAndUpdate(any(), any(), any(), any(), any());
    }

    // Helper methods for building mock data

    private BulkUpload buildBulkUpload() {
        BulkUpload bulkUpload = new BulkUpload();
        bulkUpload.setBulkUploadId(100L);
        bulkUpload.setCreateUserId("svcDocUser1");
        bulkUpload.setDocumentExpectedCount(2); // Assume 2 documents in the batch
        bulkUpload.setBulkUploadGUID(bulkUploadGuid);
        return bulkUpload;
    }

    private List<BulkUploadDocuments> buildBulkUploadDocuments() {
        List<BulkUploadDocuments> bulkUploadDocuments = new ArrayList<>();

        // Document 1
        BulkUploadDocuments doc1 = new BulkUploadDocuments();
        doc1.setBulkUploadId(1L);
        doc1.setCreateUserId(authUserId);
        doc1.setPublishFl(true);
        doc1.setErrorFl(false);
        doc1.setDocument(new Document("doc1", 1L)); // Document with ID 1
        doc1.setMetadataValidatedFl(true);
        bulkUploadDocuments.add(doc1);

        // Document 2 (with error)
        BulkUploadDocuments doc2 = new BulkUploadDocuments();
        doc2.setBulkUploadId(2L);
        doc2.setCreateUserId(authUserId);
        doc2.setPublishFl(false);
        doc2.setErrorFl(true);
        doc2.setBulkUploadErrorId(20L);
        doc2.setBulkUploadError(buildBUError());
        doc2.setDocument(new Document("doc2", 2L)); // Document with ID 2
        bulkUploadDocuments.add(doc2);

        return bulkUploadDocuments;
    }

    private BulkUploadError buildBUError() {
        BulkUploadError error = new BulkUploadError();
        error.setBulkUploadErrorId(20L);
        error.setErrorCode("INTERNAL_ERROR");
        error.setErrorDesc("Internal Network Error, Document cannot be accepted at this time");
        return error;
    }
}
