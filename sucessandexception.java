@Test
public void testPublishPushToUESuccess() {
    // Set up the UserEntitlements
    getUserEntitlements();

    // Create the BulkUpload object
    bulkUpload = buildBulkUpload();
    bulkUpload.setCreateUserId(authUserId);
    bulkUpload.setDocumentExpectedCount(1);
    bulkUpload.setBulkUploadGUID(bulkUploadGuid);

    // Mock behavior for bulkUploadRepository and related components
    when(bulkUploadRepository.findByBulkUploadGUID(bulkUploadGuid)).thenReturn(bulkUpload);

    // Mock the BulkUploadDocuments repository to return a list of documents
    List<BulkUploadDocuments> bulkUploadDocuments = buildBulkUploadDocuments();
    when(bulkUploadDocumentsRepository.findAllByBulkUploadId(anyLong())).thenReturn(bulkUploadDocuments);

    // Mock the save methods
    when(documentService.saveAll(anyList())).thenReturn(new ArrayList<>());
    when(bulkUploadDocumentsRepository.saveAll(anyList())).thenReturn(bulkUploadDocuments);
    when(bulkUploadRepository.save(any())).thenReturn(bulkUpload);

    // Mock the UtilsService to simulate a successful push to UE
    doNothing().when(utilsService).pushToUEAndUpdate(anyString(), any(UserEntitlements.class), anyList(), any(), any());

    // Mock the ScheduledThreadPoolExecutor to just execute the task directly
    ScheduledFuture<?> mockScheduledFuture = mock(ScheduledFuture.class);
    when(scheduledThreadPoolExecutor.schedule(any(Runnable.class), eq(0L), eq(TimeUnit.MILLISECONDS))).thenReturn(mockScheduledFuture);

    try {
        // Call the method under test
        BulkUploadPublishResponseData response = bulkUploadPublishService.publishBulkUpload(bulkUploadGuid, authUserId, requestTrackingId);

        // Assertions
        assertNotNull(response);
        assertNotNull(response.getData());
        assertNotNull(response.getData().getBulkUploadDocuments());

        // Verify that the pushToUEAndUpdate method was called
        verify(utilsService, times(1)).pushToUEAndUpdate(eq(requestTrackingId), eq(userEntitlements), anyList(), any(), any());

        // Verify the interactions with the repository and services
        verify(documentService, times(1)).saveAll(anyList());
        verify(bulkUploadDocumentsRepository, times(1)).saveAll(anyList());
        verify(bulkUploadRepository, times(1)).save(any());
        verify(scheduledThreadPoolExecutor, times(1)).schedule(any(Runnable.class), eq(0L), eq(TimeUnit.MILLISECONDS));
    } catch (DocException e) {
        fail("Exception should not have been thrown");
    }
}

@Test
public void testPublishPushToUEException() {
    // Set up the UserEntitlements
    getUserEntitlements();

    // Create the BulkUpload object
    bulkUpload = buildBulkUpload();
    bulkUpload.setCreateUserId(authUserId);
    bulkUpload.setDocumentExpectedCount(1);
    bulkUpload.setBulkUploadGUID(bulkUploadGuid);

    // Mock behavior for bulkUploadRepository and related components
    when(bulkUploadRepository.findByBulkUploadGUID(bulkUploadGuid)).thenReturn(bulkUpload);

    // Mock the BulkUploadDocuments repository to return a list of documents
    List<BulkUploadDocuments> bulkUploadDocuments = buildBulkUploadDocuments();
    when(bulkUploadDocumentsRepository.findAllByBulkUploadId(anyLong())).thenReturn(bulkUploadDocuments);

    // Mock the save methods
    when(documentService.saveAll(anyList())).thenReturn(new ArrayList<>());
    when(bulkUploadDocumentsRepository.saveAll(anyList())).thenReturn(bulkUploadDocuments);
    when(bulkUploadRepository.save(any())).thenReturn(bulkUpload);

    // Mock the UtilsService to throw an exception when calling pushToUEAndUpdate
    doThrow(new DocException("Error pushing to UE")).when(utilsService).pushToUEAndUpdate(anyString(), any(UserEntitlements.class), anyList(), any(), any());

    // Mock the ScheduledThreadPoolExecutor to just execute the task directly
    ScheduledFuture<?> mockScheduledFuture = mock(ScheduledFuture.class);
    when(scheduledThreadPoolExecutor.schedule(any(Runnable.class), eq(0L), eq(TimeUnit.MILLISECONDS))).thenReturn(mockScheduledFuture);

    try {
        // Call the method under test
        BulkUploadPublishResponseData response = bulkUploadPublishService.publishBulkUpload(bulkUploadGuid, authUserId, requestTrackingId);

        // Assertions
        assertNotNull(response);
        assertNotNull(response.getData());
        assertNotNull(response.getData().getBulkUploadDocuments());

        // Verify that the pushToUEAndUpdate method was called
        verify(utilsService, times(1)).pushToUEAndUpdate(eq(requestTrackingId), eq(userEntitlements), anyList(), any(), any());

        // Verify the interactions with the repository and services
        verify(documentService, times(1)).saveAll(anyList());
        verify(bulkUploadDocumentsRepository, times(1)).saveAll(anyList());
        verify(bulkUploadRepository, times(1)).save(any());
        verify(scheduledThreadPoolExecutor, times(1)).schedule(any(Runnable.class), eq(0L), eq(TimeUnit.MILLISECONDS));

    } catch (DocException e) {
        // Ensure the exception is correctly thrown and handled
        assertEquals("Error pushing to UE", e.getMessage());
    }
}
