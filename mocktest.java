import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.*;

public class BulkUploadServiceTest {

    @Mock
    private UtilsService utilsService;

    @Mock
    private ExecutorService executorService;

    @InjectMocks
    private BulkUploadService bulkUploadService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Initialize mocks
        executorService = mock(ExecutorService.class);
    }

    @Test
    void testPublishBulkUpload_withValidData() throws InterruptedException {
        // Create sample Document objects
        Document document1 = createMockDocument(1L, "Document 1", 100L);
        Document document2 = createMockDocument(2L, "Document 2", 200L);
        Document document3 = createMockDocument(3L, "Document 3", 300L);
        
        List<Document> docList = Arrays.asList(document1, document2, document3);
        
        String requestTrackingId = "tracking-id-123";
        UserEntitlements userEntitlements = mock(UserEntitlements.class);
        
        // Mock behavior for ExecutorService
        doNothing().when(executorService).submit(any(Runnable.class)); // Mock submit to do nothing
        
        // Call the method
        bulkUploadService.publishBulkUpload(requestTrackingId, userEntitlements, docList);

        // Verify that the executorService.submit method was called for each batch
        verify(executorService, times(1)).submit(any(Runnable.class));
        
        // Ensure utilsService.pushToUEAndUpdate is called for each batch (mocked)
        verify(utilsService, times(1)).pushToUEAndUpdate(eq(requestTrackingId), eq(userEntitlements), anyList(), any(), any());
    }

    @Test
    void testPublishBulkUpload_emptyDocList() throws InterruptedException {
        // Empty document list
        String requestTrackingId = "tracking-id-123";
        UserEntitlements userEntitlements = mock(UserEntitlements.class);
        List<Document> docList = Collections.emptyList();

        // Mock behavior for ExecutorService
        doNothing().when(executorService).submit(any(Runnable.class));

        // Call the method
        bulkUploadService.publishBulkUpload(requestTrackingId, userEntitlements, docList);

        // Verify that executorService.submit was not called as the document list is empty
        verify(executorService, never()).submit(any(Runnable.class));

        // Verify that utilsService.pushToUEAndUpdate was not called
        verify(utilsService, never()).pushToUEAndUpdate(eq(requestTrackingId), eq(userEntitlements), anyList(), any(), any());
    }

    @Test
    void testPublishBulkUpload_withExceptionInPushToUE() throws InterruptedException {
        // Create sample Document objects
        Document document1 = createMockDocument(1L, "Document 1", 100L);
        Document document2 = createMockDocument(2L, "Document 2", 200L);
        
        List<Document> docList = Arrays.asList(document1, document2);
        
        String requestTrackingId = "tracking-id-123";
        UserEntitlements userEntitlements = mock(UserEntitlements.class);
        
        // Mock behavior for ExecutorService
        doNothing().when(executorService).submit(any(Runnable.class));

        // Simulate an exception thrown by pushToUEAndUpdate
        doThrow(new DocException("Error during push")).when(utilsService).pushToUEAndUpdate(anyString(), any(UserEntitlements.class), anyList(), any(), any());

        // Call the method
        bulkUploadService.publishBulkUpload(requestTrackingId, userEntitlements, docList);

        // Verify that executorService.submit was called
        verify(executorService, times(1)).submit(any(Runnable.class));

        // Verify that utilsService.pushToUEAndUpdate was called
        verify(utilsService, times(1)).pushToUEAndUpdate(eq(requestTrackingId), eq(userEntitlements), anyList(), any(), any());
    }

    @Test
    void testShutdownExecutorService() throws InterruptedException {
        // Create sample Document objects
        Document document1 = createMockDocument(1L, "Document 1", 100L);
        Document document2 = createMockDocument(2L, "Document 2", 200L);
        
        List<Document> docList = Arrays.asList(document1, document2);
        
        String requestTrackingId = "tracking-id-123";
        UserEntitlements userEntitlements = mock(UserEntitlements.class);
        
        // Mock behavior for ExecutorService
        doNothing().when(executorService).submit(any(Runnable.class));
        when(executorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true); // Simulate successful shutdown

        // Call the method
        bulkUploadService.publishBulkUpload(requestTrackingId, userEntitlements, docList);

        // Verify that shutdown was called after all tasks were submitted
        verify(executorService, times(1)).shutdown();
        verify(executorService, times(1)).awaitTermination(anyLong(), eq(TimeUnit.SECONDS));
    }

    // Helper method to create a mock Document object
    private Document createMockDocument(Long id, String name, Long size) {
        Document document = new Document();
        document.setDocumentId(id);
        document.setDocumentName(name);
        document.setSizeInBytes(size);
        
        // Mock additional fields (you can adjust these according to your actual class)
        document.setDocumentType(mock(DocumentType.class));
        document.setDocumentMimeTypes(mock(DocumentMimeTypes.class));
        document.setDocPathId(mock(DocumentPath.class));
        
        return document;
    }
}
