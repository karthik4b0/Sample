public void processDocumentsInBatches(List<Document> docList, int batchSize, int simultaneousRequests, String requestTrackingId, String userEntitlements) {
    if (!CollectionUtils.isNullOrEmpty(docList)) {
        // Partition the documents into smaller batches based on batchSize
        List<List<Document>> docPartitions = ListUtils.partition(docList, batchSize);

        // Create an ExecutorService with the number of simultaneous requests
        ExecutorService executorService = Executors.newFixedThreadPool(simultaneousRequests);

        // For each batch of documents, submit a task to the executor service
        for (List<Document> docBatch : docPartitions) {
            executorService.submit(() -> {
                try {
                    // Push the document batch to UE and update
                    utilsService.pushToUEAndUpdate(requestTrackingId, userEntitlements, docBatch, new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
                    logger.info("Completed pushToUEAndUpdate processing for batch of documents.");
                } catch (DocException e) {
                    logger.error("Exception occurred while pushing to UE.", e);
                } catch (InterruptedException e) {
                    logger.error("Interrupted Exception occurred while pushing to UE.", e);
                }
            });
        }

        // Gracefully shut down the executor service after all tasks have been submitted
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}
