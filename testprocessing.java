@Test
public void testPublishPushToUESuccess() {
    getUserEntitlements();
    bulkUpload = buildBulkUpload();
    bulkUpload.setCreateUserId(authUserId);
    bulkUpload.setDocumentExpectedCount(1);
    bulkUpload.setBulkUploadGUID(bulkUploadGuid);
    
    // Mocking necessary dependencies
    when(bulkUploadRepository.findByBulkUploadGUID(bulkUploadGuid)).thenReturn(bulkUpload);
    List<BulkUploadDocuments> bulkUploadDocuments = buildBulkUploadGuid();
    when(bulkUploadDocumentsRepository.findAllByBulkUploadId(anyLong())).thenReturn(bulkUploadDocuments);
    
    // Mocking services
    when(documentService.saveAll(anyList())).thenReturn(new ArrayList<>());
    when(bulkUploadDocumentsRepository.saveAll(anyList())).thenReturn(bulkUploadDocuments);
    when(bulkUploadRepository.save(any())).thenReturn(bulkUpload);
    
    try {
        // Calling the method under test
        BulkUploadPublishResponseData test = bulkUploadPublishService.publishBulkUpload(bulkUploadGuid, authUserId, bulkUploadGuid);
        
        assertNotNull(test);
        assertNotNull(test.getData());
        assertNotNull(test.getData().getBulkUploadDocuments());
        
        assertEquals(bulkUploadGuid, test.getData().getBulkUploadGuid());
        
        // Checking if isPublished flags are set correctly after processing
        assertEquals(true, test.getData().getBulkUploadDocuments().get(0).isPublished());  // First document
        assertEquals(false, test.getData().getBulkUploadDocuments().get(1).isPublished()); // Second document
        
        // Verifying the calls to repositories
        verify(bulkUploadErrorRepository, atMost(1)).findOneByErrorCode(captor.capture());
        verify(documentService, atMost(1)).saveAll(any());
        verify(bulkUploadDocumentsRepository, atMost(1)).saveAll(any());
    } catch (DocException | Exception e) {
        fail("Exception occurred: " + e.getMessage());
    }
}

@Test
public void testPublishPushToUEException() {
    getUserEntitlements();
    bulkUpload = buildBulkUpload();
    bulkUpload.setCreateUserId(authUserId);
    bulkUpload.setDocumentExpectedCount(1);
    bulkUpload.setBulkUploadGUID(bulkUploadGuid);
    
    // Mocking necessary dependencies
    when(bulkUploadRepository.findByBulkUploadGUID(bulkUploadGuid)).thenReturn(bulkUpload);
    List<BulkUploadDocuments> bulkUploadDocuments = buildBulkUploadGuid();
    when(bulkUploadDocumentsRepository.findAllByBulkUploadId(anyLong())).thenReturn(bulkUploadDocuments);
    
    // Mocking services
    when(bulkUploadErrorRepository.findOneByErrorCode(anyString())).thenReturn(buildBUError());
    when(documentService.saveAll(anyList())).thenReturn(new ArrayList<>());
    when(bulkUploadDocumentsRepository.saveAll(anyList())).thenReturn(bulkUploadDocuments);
    when(bulkUploadRepository.save(any())).thenReturn(bulkUpload);
    
    try {
        // Calling the method under test
        BulkUploadPublishResponseData test = bulkUploadPublishService.publishBulkUpload(bulkUploadGuid, authUserId, bulkUploadGuid);
        
        assertNotNull(test);
        assertNotNull(test.getData());
        assertNotNull(test.getData().getBulkUploadDocuments());
        
        assertEquals(bulkUploadGuid, test.getData().getBulkUploadGuid());
        
        // Verifying that the first document was published and second wasn't (due to error)
        assertEquals(true, test.getData().getBulkUploadDocuments().get(0).isPublished());  // First document
        assertEquals(false, test.getData().getBulkUploadDocuments().get(1).isPublished()); // Second document due to error
        
        // Verifying repository calls
        verify(bulkUploadErrorRepository, atMost(1)).findOneByErrorCode(captor.capture());
        verify(documentService, atMost(1)).saveAll(any());
    } catch (DocException | Exception e) {
        fail("Exception occurred: " + e.getMessage());
    }
}

private List<BulkUploadDocuments> buildBulkUploadGuid() {
    ArrayList<BulkUploadDocuments> bulkUploadList = new ArrayList<>();
    
    BulkUploadDocuments bulkUploadDocuments = new BulkUploadDocuments();
    bulkUploadDocuments.setBulkUploadId(1L);
    bulkUploadDocuments.setCreateUserId(authUserId);
    bulkUploadDocuments.setPublishFl(true); // Document to be published successfully
    bulkUploadDocuments.setErrorFl(false); // No error
    bulkUploadDocuments.setDocument(new Document());
    bulkUploadDocuments.setMetadataValidatedFl(true);
    bulkUploadList.add(bulkUploadDocuments);
    
    BulkUploadDocuments bulkUploadDocuments1 = new BulkUploadDocuments();
    bulkUploadDocuments1.setBulkUploadId(2L);
    bulkUploadDocuments1.setCreateUserId(authUserId);
    bulkUploadDocuments1.setPublishFl(false); // Document with publish failure
    bulkUploadDocuments1.setErrorFl(true); // Error flag set to true
    bulkUploadDocuments1.setBulkUploadError(buildBUError());
    bulkUploadDocuments1.setDocument(new Document());
    bulkUploadList.add(bulkUploadDocuments1);
    
    return bulkUploadList;
}

@Test
public void testPublishPushTOUESuccess() {
    getUserEntitlements();

    // Create a BulkUpload object and set up necessary properties
    bulkUpload = buildBulkUpload();
    bulkUpload.setCreateUserId(authUserId);
    bulkUpload.setDocumentExpectedCount(1); // Example document count
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

    try {
        // Execute the method under test
        BulkUploadPublishResponseData response = bulkUploadPublishService.publishBulkUpload(bulkUploadGuid, authUserId, bulkUploadGuid);

        // Assertions to check if the BulkUploadDocuments were updated correctly
        assertNotNull(response);
        assertNotNull(response.getData());
        assertNotNull(response.getData().getBulkUploadDocuments());
        assertEquals(bulkUploadGuid, response.getData().getBulkUploadGuid());

        // Verify that document 0 was published
        assertTrue(response.getData().getBulkUploadDocuments().get(0).isPublished());

        // Verify that document 1 was not published due to error
        assertFalse(response.getData().getBulkUploadDocuments().get(1).isPublished());

        // Check error code for document 1
        assertNotNull(response.getData().getBulkUploadDocuments().get(1).getErrorCode());
        assertNotNull(response.getData().getBulkUploadDocuments().get(1).getErrorMessage());

        // Verify interactions with repositories
        verify(bulkUploadErrorRepository, atMost(1)).findOneByErrorCode(anyString());
        verify(documentService, atMost(1)).saveAll(anyList());
        verify(bulkUploadDocumentsRepository, atMost(1)).saveAll(anyList());
    } catch (DocException e) {
        fail("Exception occurred during bulk upload: " + e.getMessage());
    }
}

