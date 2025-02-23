@Test
public void testPublishPushTOUESuccess() {
    getUserEntitlements();
    bulkUpload = buildBulkUpload();
    bulkUpload.setCreateUserId(authUserId);
    bulkUpload.setDocumentExpectedCount(1);
    bulkUpload.setBulkUploadGUID(bulkUploadGuid);

    // Mock the repository to return the created bulkUpload object
    when(bulkUploadRepository.findByBulkUploadGUID(bulkUploadGuid)).thenReturn(bulkUpload);

    // Mock the BulkUploadDocuments to simulate data retrieval
    List<BulkUploadDocuments> bulkUploadDocuments = buildBulkUploadDocuments();
    when(bulkUploadDocumentsRepository.findAllByBulkUploadId(anyLong())).thenReturn(bulkUploadDocuments);

    // Mock saving of BulkUploadDocuments and documents
    when(documentService.saveAll(anyList())).thenReturn(new ArrayList<>());
    when(bulkUploadDocumentsRepository.saveAll(anyList())).thenReturn(bulkUploadDocuments);
    when(bulkUploadRepository.save(any())).thenReturn(bulkUpload);

    // Mock ExecutorService to execute tasks synchronously
    ExecutorService mockExecutorService = mock(ExecutorService.class);
    doAnswer(invocation -> {
        Runnable task = invocation.getArgument(0);
        task.run(); // Execute the task immediately
        return null;
    }).when(mockExecutorService).submit(any(Runnable.class));

    // Set the mocked ExecutorService to your service
    bulkUploadPublishService.setExecutorService(mockExecutorService);

    try {
        // Execute the method under test
        BulkUploadPublishResponseData response = bulkUploadPublishService.publishBulkUpload(bulkUploadGuid, authUserId, bulkUploadGuid, getdqlrequest());

        // Make sure response and its data are not null
        assertNotNull(response);
        assertNotNull(response.getData());
        assertNotNull(response.getData().getBulkUploadDocuments());

        // Assertions for the BulkUploadDocuments' publish status
        assertEquals(true, response.getData().getBulkUploadDocuments().get(0).isPublished()); // First document should be published
        assertEquals(false, response.getData().getBulkUploadDocuments().get(1).isPublished()); // Second document should NOT be published

        // Assert error codes and messages for the second document (which should have an error)
        assertNull(response.getData().getBulkUploadDocuments().get(0).getErrorCode());
        assertNotNull(response.getData().getBulkUploadDocuments().get(1).getErrorCode());
        assertNull(response.getData().getBulkUploadDocuments().get(0).getErrorMessage());
        assertNotNull(response.getData().getBulkUploadDocuments().get(1).getErrorMessage());

        // Verify interactions with repositories and services
        verify(bulkUploadErrorRepository, atMost(1)).findOneByErrorCode(anyString());
        verify(documentService, atMost(1)).saveAll(anyList());
        verify(bulkUploadDocumentsRepository, atMost(1)).saveAll(anyList());
    } catch (DocException e) {
        fail("Exception occurred during bulk upload: " + e.getMessage());
    } catch (Exception e) {
        fail("Unexpected exception occurred during bulk upload: " + e.getMessage());
    }
}
