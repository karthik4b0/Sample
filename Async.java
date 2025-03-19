@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public Executor taskExecutor() {
        // This creates a fixed thread pool with the number of simultaneous requests (threads)
        return Executors.newFixedThreadPool(8); // 8 simultaneous requests
    }
}

@Service
public class DocumentService {

    private final UtilsService utilsService; // Assuming utilsService is injected here

    @Autowired
    public DocumentService(UtilsService utilsService) {
        this.utilsService = utilsService;
    }

    public void processDocumentsInBatches(List<Document> docList, int batchSize, int simultaneousRequests, String requestTrackingId, String userEntitlements) {
        if (!CollectionUtils.isNullOrEmpty(docList)) {
            // Partition the documents into smaller batches based on batchSize
            List<List<Document>> docPartitions = ListUtils.partition(docList, batchSize);

            // For each batch of documents, submit a task to be executed asynchronously
            for (List<Document> docBatch : docPartitions) {
                processBatchAsync(docBatch, requestTrackingId, userEntitlements);
            }
        }
    }

    @Async
    public void processBatchAsync(List<Document> docBatch, String requestTrackingId, String userEntitlements) {
        try {
            // Push the document batch to UE and update
            utilsService.pushToUEAndUpdate(requestTrackingId, userEntitlements, docBatch, new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
            logger.info("Completed pushToUEAndUpdate processing for batch of documents.");
        } catch (DocException e) {
            logger.error("Exception occurred while pushing to UE.", e);
        } catch (InterruptedException e) {
            logger.error("Interrupted Exception occurred while pushing to UE.", e);
        }
    }
}

