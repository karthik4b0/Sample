package com.seic.docmgmt.dql.services.v2;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.seic.docmgmt.dql.model.primary.*;
import com.seic.docmgmt.dql.repositories.primary.*;
import com.seic.docmgmt.dql.services.UtilsService;
import com.seic.docmgmt.dql.model.v2.UserEntitlements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class BulkUploadPublishServiceTest {

    @InjectMocks
    private BulkUploadPublishService bulkUploadPublishService;

    @Mock
    private ExecutorService executorService;

    @Mock
    private UtilsService utilsService;

    @Mock
    private PropertyConfig propertyConfig;

    @Mock
    private BulkUploadRepository bulkUploadRepository;

    @Mock
    private BulkUploadDocumentsRepository bulkUploadDocumentsRepository;

    @Mock
    private BulkUploadErrorRepository bulkUploadErrorRepository;

    @Mock
    private DocumentService documentService;

    @Mock
    private DQLRequest dqlRequest;

    private String bulkUploadGuid = UUID.randomUUID().toString();
    private String requestTrackingId = UUID.randomUUID().toString();
    private String authUserId = "svcDocUser";

    @Before
    public void setup() {
        // Mocking basic setup
        when(propertyConfig.getPackagesSvcAcctUserName()).thenReturn("svcDocUser");
        when(propertyConfig.getPackagesSvcAcctUserProfileCode()).thenReturn("svcDocUserProfileCode");
    }

    @Test
    public void testPublishBulkUpload_withMultipleRequests() {
        // Prepare mock data
        BulkUpload bulkUpload = buildBulkUpload();
        List<Document> documents = buildDocuments(5);  // Create 5 documents

        when(bulkUploadRepository.findByBulkUploadGUID(bulkUploadGuid)).thenReturn(bulkUpload);
        when(bulkUploadDocumentsRepository.findAllByBulkUploadId(anyLong())).thenReturn(createBulkUploadDocuments());

        // Simulate successful pushToUEAndUpdate call
        doNothing().when(utilsService).pushToUEAndUpdate(anyString(), any(), anyList(), any(), any());

        try {
            // Call the method under test
            bulkUploadPublishService.publishBulkUpload(bulkUploadGuid, authUserId, requestTrackingId, dqlRequest);

            // Verifications
            verify(executorService, times(1)).submit(any());
            verify(utilsService, times(1)).pushToUEAndUpdate(anyString(), any(), anyList(), any(), any());
        } catch (Exception e) {
            fail("Exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void testPublishBulkUpload_withPushToUEException() {
        // Prepare mock data
        BulkUpload bulkUpload = buildBulkUpload();
        List<Document> documents = buildDocuments(5);  // Create 5 documents

        when(bulkUploadRepository.findByBulkUploadGUID(bulkUploadGuid)).thenReturn(bulkUpload);
        when(bulkUploadDocumentsRepository.findAllByBulkUploadId(anyLong())).thenReturn(createBulkUploadDocuments());

        // Simulate pushToUEAndUpdate exception
        doThrow(new RuntimeException("Simulated exception")).when(utilsService).pushToUEAndUpdate(anyString(), any(), anyList(), any(), any());

        try {
            // Call the method under test
            bulkUploadPublishService.publishBulkUpload(bulkUploadGuid, authUserId, requestTrackingId, dqlRequest);
            fail("Exception should have been thrown");
        } catch (RuntimeException e) {
            assertEquals("Simulated exception", e.getMessage());
        }
    }

    @Test
    public void testPublishBulkUpload_withNullUserEntitlements() {
        try {
            // Call the method under test with null userEntitlements
            bulkUploadPublishService.publishBulkUpload(bulkUploadGuid, authUserId, requestTrackingId, dqlRequest);
            fail("DocException should have been thrown");
        } catch (DocException e) {
            assertEquals(e.getHttpStatus(), HttpStatus.UNAUTHORIZED);
            assertEquals(e.getUserMessage(), "User Entitlements Required.");
            assertEquals(e.getTrackingId(), requestTrackingId);
        }
    }

    @Test
    public void testPublishBulkUpload_withNullRequestTrackingId() {
        try {
            // Call the method under test with null requestTrackingId
            bulkUploadPublishService.publishBulkUpload(bulkUploadGuid, authUserId, null, dqlRequest);
            fail("DocException should have been thrown");
        } catch (DocException e) {
            assertEquals(e.getHttpStatus(), HttpStatus.BAD_REQUEST);
            assertEquals(e.getUserMessage(), "Request Tracking ID Required.");
        }
    }

    @Test
    public void testPublishBulkUpload_withNullDocList() {
        try {
            // Call the method under test with an empty document list
            bulkUploadPublishService.publishBulkUpload(bulkUploadGuid, authUserId, requestTrackingId, null);
            fail("DocException should have been thrown");
        } catch (DocException e) {
            assertEquals(e.getHttpStatus(), HttpStatus.BAD_REQUEST);
            assertEquals(e.getUserMessage(), "DQL Request Required.");
        }
    }

    // Helper Methods

    private BulkUpload buildBulkUpload() {
        BulkUpload bulkUpload = new BulkUpload();
        bulkUpload.setBulkUploadGUID(bulkUploadGuid);
        bulkUpload.setCreateUserId(authUserId);
        return bulkUpload;
    }

    private List<Document> buildDocuments(int count) {
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Document document = new Document();
            document.setDocumentName("Document " + i);
            document.setDocumentId((long) i);
            documents.add(document);
        }
        return documents;
    }

    private List<BulkUploadDocuments> createBulkUploadDocuments() {
        List<BulkUploadDocuments> documents = new ArrayList<>();
        BulkUploadDocuments doc = new BulkUploadDocuments();
        doc.setBulkUploadId(1L);
        doc.setCreateUserId(authUserId);
        doc.setDocument(new Document());
        documents.add(doc);
        return documents;
    }

    private BulkUploadError buildBUError() {
        BulkUploadError error = new BulkUploadError();
        error.setBulkUploadErrorId(20L);
        error.setErrorCode("INTERNAL_ERROR");
        error.setErrorDesc("Internal Network Error, Document cannot be accepted at this time");
        return error;
    }
}
