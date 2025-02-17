
// Maximum number of documents to send per request (5 documents per request)
public static final int UE_PUSH_MAX_DOCUMENT_SIZE = 5;

// Number of simultaneous requests (8 requests at once)
public static final int UE_PUSH_SIMULTANEOUS_REQUESTS = 8;

// Total number of documents to send in one batch (5 documents * 8 requests = 40 documents)
public static final int UE_PUSH_TOTAL_DOCUMENTS_IN_BATCH = UE_PUSH_MAX_DOCUMENT_SIZE * UE_PUSH_SIMULTANEOUS_REQUESTS;

public BulkUploadPublishResponseData publishBulkUpload(String bulkUploadGuid, String authUserId, String requestTrackingId) {
    UserEntitlements userEntitlements = getUserEntitlements();

    BulkUploadPublishResponseData bulkUploadPublishResponseData = new BulkUploadPublishResponseData();

    BulkUpload aBulkUpload = validateParamsAndGetBulkUpload(bulkUploadGuid, authUserId, requestTrackingId);

    List<BulkUploadDocuments> allByBulkUploadId = bulkUploadDocumentsRepository.findAllByBulkUploadId(aBulkUpload.getBulkUploadId());
    List<BulkUploadDocuments> metaDataValidatedList = allByBulkUploadId.stream()
            .filter(p -> p.isMetadataValidatedF1() && !p.isErrorF1())
            .collect(Collectors.toList());

    List<Document> docList = new ArrayList<>();

    if (!CollectionUtils.isNullOrEmpty(metaDataValidatedList)) {
        metaDataValidatedList.forEach(uploadDocuments -> {
            Document document = uploadDocuments.getDocument();
            document.setVisibleFl(true);
            document.setStagedF1(false);
            document.setDeletedFl(false);
            document.setLastChangedDate(new Date());
            uploadDocuments.setPublishFl(true);
            bulkUploadDocumentsRepository.save(uploadDocuments);
            uploadDocuments.setDocument(document);
            docList.add(document);
        });
        documentService.saveAll(docList);
        bulkUploadDocumentsRepository.saveAll(allByBulkUploadId);
        aBulkUpload.setPublishFl(true);
        aBulkUpload.setUpdateDate(new Date());
        bulkUploadRepository.save(aBulkUpload);

        // Use constants from DocConstants for configuration
        final int MAX_DOCUMENTS_PER_REQUEST = DocConstants.UE_PUSH_MAX_DOCUMENT_SIZE; // 5 documents per request
        final int SIMULTANEOUS_REQUESTS = DocConstants.UE_PUSH_SIMULTANEOUS_REQUESTS; // 8 simultaneous requests
        final int TOTAL_DOCUMENTS_IN_BATCH = DocConstants.UE_PUSH_TOTAL_DOCUMENTS_IN_BATCH; // 40 documents in total

        // Ensure total documents in a batch are processed (i.e., 40 documents)
        if (!CollectionUtils.isNullOrEmpty(docList)) {
            // Use a fixed thread pool to limit the number of parallel requests
            ExecutorService executorService = Executors.newFixedThreadPool(SIMULTANEOUS_REQUESTS);

            // Partition the documents into chunks of 40 (total batch size)
            List<List<Document>> documentBatches = ListUtils.partition(docList, TOTAL_DOCUMENTS_IN_BATCH);

            // Submit tasks for processing each batch of 40 documents
            List<Future<?>> futures = new ArrayList<>();
            for (List<Document> batch : documentBatches) {
                Future<?> future = executorService.submit(() -> {
                    try {
                        // Split each batch into smaller chunks of 5 documents (per request)
                        List<List<Document>> smallBatches = ListUtils.partition(batch, MAX_DOCUMENTS_PER_REQUEST);

                        // Process each small batch (of 5 documents) in parallel
                        List<Future<?>> batchFutures = new ArrayList<>();
                        for (List<Document> smallBatch : smallBatches) {
                            batchFutures.add(executorService.submit(() -> {
                                try {
                                    // Push the batch to UE and update
                                    utilsService.pushTOUEAndUpdate(requestTrackingId, userEntitlements, smallBatch, new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
                                    logger.info("Completed pushTOUEAndUpdate processing for small batch of size: " + smallBatch.size());
                                } catch (Exception e) {
                                    logger.error("Error occurred while processing small batch.", e);
                                }
                            }));
                        }

                       //This piece of code is similar to the previous one you asked about, but specifically handles waiting for the completion of smaller batches of tasks, each representing a batch of 5 documents being pushed to UE (User Entitlements).
                        // Wait for all small batches to complete
                        for (Future<?> smallBatchFuture : batchFutures) {
                            smallBatchFuture.get();
                        }

                    } catch (Exception e) {
                        logger.error("Error occurred while processing large batch.", e);
                    }
                });
				//is adding a Future object (represented by the variable future) to a collection, specifically a list or set named futures.
                futures.add(future);
            }

//This piece of code is responsible for blocking the execution of the current thread until all the asynchronous tasks (represented by Future objects) in the futures list are completed. Let's break it down step by step:
            // Wait for all batch tasks to complete
            for (Future<?> future : futures) {
                try {
                    future.get(); // block until the task completes
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("Error while waiting for batch processing to complete.", e);
                }
            }


            // Shutdown executor after use
            executorService.shutdown();
        }
    }

    BulkUploadPublishResponse bulkUploadPublishResponse = new BulkUploadPublishResponse();
    bulkUploadPublishResponse.setBulkUploadGuid(aBulkUpload.getBulkUploadGUID());
    bulkUploadPublishResponse.setBulkUploadDocuments(new ArrayList<>());
    for (BulkUploadDocuments bulkUploadDocument : allByBulkUploadId) {
        buildBulkUploadPublishResponse(bulkUploadPublishResponse, bulkUploadDocument);
    }
    bulkUploadPublishResponseData.setData(bulkUploadPublishResponse);
    return bulkUploadPublishResponseData;
}

