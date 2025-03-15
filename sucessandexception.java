@RunWith(MockitoJUnitRunner.class)
public class BulkUploadServiceTest {

    @Mock
    private BulkUploadDocumentsRepository bulkUploadDocumentsRepository;

    @Mock
    private UtilsService utilsService;

    @Mock
    private DocumentService documentService;

    @Mock
    private BulkUploadRepository bulkUploadRepository;

    @Mock
    private UserEntitlements userEntitlements;

    @InjectMocks
    private BulkUploadService bulkUploadService;

    private static final String BULK_UPLOAD_GUID = "some-guid";
    private static final String AUTH_USER_ID = "user123";
    private static final String REQUEST_TRACKING_ID = "track123";
    private static final int BATCH_SIZE = 10;
    private static final int SIMULTANEOUS_REQUESTS = 3;

    @Test
    public void testPublishBulkUpload_Success() throws Exception {
        // Mocking data
        BulkUpload bulkUpload = new BulkUpload();
        bulkUpload.setBulkUploadId(1L);
        bulkUpload.setBulkUploadGUID(BULK_UPLOAD_GUID);
        bulkUpload.setPublishFl(false);

        BulkUploadDocuments bulkUploadDocuments = new BulkUploadDocuments();
        bulkUploadDocuments.setBulkUploadId(bulkUpload.getBulkUploadId());
        bulkUploadDocuments.setMetadataValidatedF1(true);
        bulkUploadDocuments.setErrorF1(false);
        Document document = new Document();
        bulkUploadDocuments.setDocument(document);
        List<BulkUploadDocuments> allByBulkUploadId = Arrays.asList(bulkUploadDocuments);

        // Mocking methods
        when(bulkUploadDocumentsRepository.findAllByBulkUploadId(anyLong())).thenReturn(allByBulkUploadId);
        when(bulkUploadRepository.save(any(BulkUpload.class))).thenReturn(bulkUpload);
        when(documentService.saveAll(anyList())).thenReturn(new ArrayList<>());

        // Call the method under test
        BulkUploadPublishResponseData response = bulkUploadService.publishBulkUpload(BULK_UPLOAD_GUID, AUTH_USER_ID, REQUEST_TRACKING_ID);

        // Verifying the results
        assertNotNull(response);
        assertEquals(BULK_UPLOAD_GUID, response.getData().getBulkUploadGuid());
        verify(bulkUploadDocumentsRepository, times(1)).save(any(BulkUploadDocuments.class));
        verify(utilsService, times(1)).pushToUEAndUpdate(anyString(), any(), anyList(), any(), any());
    }

    @Test
    public void testPublishBulkUpload_NoDocumentsToPublish() throws Exception {
        // Mocking data with no valid documents
        List<BulkUploadDocuments> emptyList = new ArrayList<>();

        // Mocking methods
        when(bulkUploadDocumentsRepository.findAllByBulkUploadId(anyLong())).thenReturn(emptyList);

        // Call the method under test
        BulkUploadPublishResponseData response = bulkUploadService.publishBulkUpload(BULK_UPLOAD_GUID, AUTH_USER_ID, REQUEST_TRACKING_ID);

        // Verifying the results
        assertNotNull(response);
        assertTrue(response.getData().getBulkUploadDocuments().isEmpty());
        verify(bulkUploadDocumentsRepository, times(0)).save(any(BulkUploadDocuments.class));
    }

    @Test(expected = DocException.class)
    public void testPublishBulkUpload_FailureInPushToUE() throws Exception {
        // Mocking data
        BulkUpload bulkUpload = new BulkUpload();
        bulkUpload.setBulkUploadId(1L);
        bulkUpload.setBulkUploadGUID(BULK_UPLOAD_GUID);
        bulkUpload.setPublishFl(false);

        BulkUploadDocuments bulkUploadDocuments = new BulkUploadDocuments();
        bulkUploadDocuments.setBulkUploadId(bulkUpload.getBulkUploadId());
        bulkUploadDocuments.setMetadataValidatedF1(true);
        bulkUploadDocuments.setErrorF1(false);
        Document document = new Document();
        bulkUploadDocuments.setDocument(document);
        List<BulkUploadDocuments> allByBulkUploadId = Arrays.asList(bulkUploadDocuments);

        // Mocking methods
        when(bulkUploadDocumentsRepository.findAllByBulkUploadId(anyLong())).thenReturn(allByBulkUploadId);
        when(bulkUploadRepository.save(any(BulkUpload.class))).thenReturn(bulkUpload);
        when(documentService.saveAll(anyList())).thenReturn(new ArrayList<>());

        // Simulating an exception in pushToUE
        doThrow(new DocException("Error during push to UE")).when(utilsService).pushToUEAndUpdate(anyString(), any(), anyList(), any(), any());

        // Call the method under test (expecting exception)
        bulkUploadService.publishBulkUpload(BULK_UPLOAD_GUID, AUTH_USER_ID, REQUEST_TRACKING_ID);
    }

    @Test
    public void testPublishBulkUpload_ExecutorServiceHandling() throws InterruptedException {
        // Mocking data
        BulkUpload bulkUpload = new BulkUpload();
        bulkUpload.setBulkUploadId(1L);
        bulkUpload.setBulkUploadGUID(BULK_UPLOAD_GUID);
        bulkUpload.setPublishFl(false);

        BulkUploadDocuments bulkUploadDocuments = new BulkUploadDocuments();
        bulkUploadDocuments.setBulkUploadId(bulkUpload.getBulkUploadId());
        bulkUploadDocuments.setMetadataValidatedF1(true);
        bulkUploadDocuments.setErrorF1(false);
        Document document = new Document();
        bulkUploadDocuments.setDocument(document);
        List<BulkUploadDocuments> allByBulkUploadId = Arrays.asList(bulkUploadDocuments);

        // Mocking methods
        when(bulkUploadDocumentsRepository.findAllByBulkUploadId(anyLong())).thenReturn(allByBulkUploadId);
        when(bulkUploadRepository.save(any(BulkUpload.class))).thenReturn(bulkUpload);
        when(documentService.saveAll(anyList())).thenReturn(new ArrayList<>());

        // Executor Service test
        ExecutorService executorService = mock(ExecutorService.class);
        when(executorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Call the method under test
        BulkUploadPublishResponseData response = bulkUploadService.publishBulkUpload(BULK_UPLOAD_GUID, AUTH_USER_ID, REQUEST_TRACKING_ID);

        // Verifying that the shutdown method was called
        verify(executorService, times(1)).shutdown();
        verify(executorService, times(1)).awaitTermination(60, TimeUnit.SECONDS);
    }
}
