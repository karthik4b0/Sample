import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class BulkUploadPublishService {

    // Injecting values from application.yml
    @Value("${ue.bulk.push.documents.per.request}")
    private int batchSize; // Number of documents per request

    @Value("${ue.bulk.push.simultaneous.requests}")
    private int simultaneousRequests; // Number of simultaneous requests

    // Rest of the service code...

    public void publishBulkUpload(String requestTrackingId, UserEntitlements userEntitlements, List<Document> docList) {

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
                        logger.info("Completed pushTOUEAndUpdate processing for batch of documents.");
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
}
