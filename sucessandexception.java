@Test
public void testPublishPushToUESuccess() {
    getUserEntitlements();
    bulkUpload = buildBulkUpload();
    bulkUpload.setCreateUserId(authUserId);
    bulkUpload.setDocumentExpectedCount(1);
    bulkUpload.setBulkUploadGUID(bulkUploadGuid);

    when(bulkUploadRepository.findByBulkUploadGUID(bulkUploadGuid)).thenReturn(bulkUpload);
    List<BulkUploadDocuments> bulkUploadDocuments = buildBulkUploadGuid();
    when(bulkUploadDocumentsRepository.findAllByBulkUploadId(anyLong())).thenReturn(bulkUploadDocuments);
    when(documentService.saveAll(anyList())).thenReturn(new ArrayList<>());
    when(bulkUploadDocumentsRepository.saveAll(anyList())).thenReturn(bulkUploadDocuments);
    when(bulkUploadRepository.save(any())).thenReturn(bulkUpload);

    // Create a mock ExecutorService
    ExecutorService mockExecutorService = mock(ExecutorService.class);
    when(mockExecutorService.submit(any(Runnable.class))).thenReturn(null);

    try {
        // Call the publishBulkUpload method, which internally uses the ExecutorService
        BulkUploadPublishResponseData test = bulkUploadPublishService.publishBulkUpload(bulkUploadGuid, authUserId, bulkUploadGuid, getdqlrequest());

        // Assertions to ensure correct response
        assertNotNull(test);
        assertNotNull(test.getData());
        assertNotNull(test.getData().getBulkUploadDocuments());
        assertEquals(bulkUploadGuid, test.getData().getBulkUploadGuid());
        assertNull(test.getData().getBulkUploadDocuments().get(0).getErrorCode());
        assertNotNull(test.getData().getBulkUploadDocuments().get(1).getErrorCode());
        assertNull(test.getData().getBulkUploadDocuments().get(0).getErrorMessage());
        assertNotNull(test.getData().getBulkUploadDocuments().get(1).getErrorMessage());
        assertEquals(true, test.getData().getBulkUploadDocuments().get(0).isPublished());
        assertEquals(false, test.getData().getBulkUploadDocuments().get(1).isPublished());

        // Verify that tasks were submitted to the ExecutorService
        verify(mockExecutorService, atMost(1)).submit(any(Runnable.class));

        // Verify repository interactions
        verify(bulkUploadErrorRepository, atMost(1)).findOneByErrorCode(captor.capture());
        verify(documentService, atMost(1)).saveAll(any());
        verify(bulkUploadDocumentsRepository, atMost(1)).saveAll(any());
    } catch (DocException e) {
        fail();
    } catch (Exception e) {
        fail();
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
