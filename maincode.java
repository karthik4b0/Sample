DQLV2Controller.java

@LogRequest
@PostMapping(value="/documents/bulkupload/{bulkUploadGuid}/publish", produces = MediaType.APPLICATION_JSON_VALUE),
 
@CrossOrigin (allowedHeaders={HEADER_X_CONTENT_TRANSFER_ENCODING},exposedHeaders ={HEADER_X_CONTENT_TRANSFER_ENCODING})
@Operation (summary = "Publish by bulk upload GUID.", description="Endpoint to publish bulkUploadGuid.") 
@ApiResponses (value = {

@ApiResponse(responseCode ="200", description = "Successful response", content =@Content (mediaType =MediaType.APPLICATION_JSON_VALUE)),
@ApiResponse(responseCode="201", description = "Successful response", content =@Content(mediaType=MediaType.APPLICATION_JSON_VALUE)),
@ApiResponse(responseCode="400", description = "Bad Request"), 
@ApiResponse(responseCode = "401", description = "Unauthorized", content =@Content),
 @ApiResponse(responseCode = "403", description = "Forbidden  - user is authorized but doesn't have access" + "to the document"),
@ApiResponse(responseCode = "404", description = "Data Not Found"),

@ApiResponse(responseCode = "500", description= "Internal Server Error", content = @Content)
})

public BulkUpload PublishResponseData bulkUploadPublish (@Parameter (description = "User calling end point") @ValidAuthUserId
@RequestHeader(value=HEADER_AUTH_USER_ID) String authUserId,
@Parameter(description= "Request tracking Id from Apigee.") @ValidRequestTrackingId
@RequestHeader(value = HEADER_REQUEST_TRACKING_ID) String requestTrackingId, 
@ValidGuid @PathVariable(value PARAM_BULK_UPLOAD_GUID) String bulkUploadGuid,
 @Parameter (hidden = true) @RequestHeader Map<String, String> allRequestHeaders, 
 @Parameter(hidden = true) @RequestParam MultiValueMap<String, String> allRequestParams
) {

return bulkUploadPublishService.publishBulkUpload (bulkUploadGuid, authUserId, requestTrackingId);
}


publishBulkUploadservice.java


public BulkUpload PublishResponseData publishBulkUpload (String bulkUploadGuid, String authUserId, String requestTrackingId) { 

UserEntitlements userEntitlements = getUserEntitlements();

BulkUploadPublishResponseData bulkUploadPublishResponseData = new BulkUpload PublishResponseData();

BulkUpload aBulkUpload = validateParamsAndGetBulkUpload (bulkUploadGuid, authUserId, requestTrackingId);

List<BulkUploadDocuments> allByBulkUploadId = bulkUploadDocuments Repository.findAllByBulkUploadId (aBulkUpload.getBulkUploadId());
List<BulkUpload Documents> metaDataValidatedList =  allByBulkUploadId.stream().filter(p-> p.isMetadataValidatedF1() && !p.isErrorF1()) .collect (Collectors.toList());

List<Document> docList = new ArrayList<>();

if (!CollectionUtils.isNullOrEmpty (metaDataValidatedList)) { 
metaDataValidatedList.forEach (uploadDocuments -> {
 Document document = uploadDocuments.getDocument(); 
 document.setVisibleFl(true);
document.setStaged F1 (false);
document.setDeletedFl(false);
document.setLastChanged Date (new Date());
uploadDocuments.setPublishFl(true);
bulkUploadDocuments Repository.save(uploadDocuments);
uploadDocuments.setDocument(document);
docList.add(document);
});
documentService.saveAll(docList);
bulkUploadDocuments Repository.saveAll(allByBulkUploadId);
aBulkUpload.setPublishFl(true);
aBulkUpload.setUpdateDate(new Date());
bulkUploadRepository.save(aBulkUpload);

-->
UE_PUSH_MAX_DOCUMENT_SIZE is constant with value 40.

if(!CollectionUtils.isNullOrEmpty(docList)) {
Listutils.partition (docList, DocConstants.UE_PUSH_MAX_DOCUMENT_SIZE). parallelStream().forEach(guid -> { 
ScheduledFuture<?> schedule = scheduledThreadPoolExecutor.schedule(
() ->{
try {
utilsService.pushToUEAndUpdate (requestTrackingId, userEntitlements, docList, new ConcurrentHashMap<>(),new ConcurrentHashMap<>());

logger.info("Completed pushTOUEAndUpdate processing");
} catch (DocException e) {
logger.error("Exception occurred, UE Error.", e);
} catch (InterruptedException e) {
logger.error("Exception occurred, UE Error.", e);
}
},
0,
TimeUnit.MILLISECONDS);

});
}
}
BulkUploadPublishResponse bulkUploadPublishResponse = new BulkUpload PublishResponse();
bulkUploadPublishResponse.setBulkUploadGuid (aBulkUpload.getBulkUploadGUID()); 
bulkUploadPublishResponse.setBulkUpload Documents (new ArrayList<>());
for (BulkUploadDocuments bulkUpload Document: allByBulkUploadId) {
    buildBulkUploadPublishResponse (bulkUploadPublishResponse, bulkUpload Document);
	}
bulkUploadPublishResponseData.setData(bulkUpload PublishResponse);
return bulkUploadPublishResponseData;
}


private BulkUpload validateParamsAndGetBulkUpload (String bulkUploadGuid, String authUserId, String requestTrackingId) { 

if (StringUtils.isNullOrEmpty(authUserId)) {
throw new DocException (HttpStatus. UNAUTHORIZED, "Auth User ID Required.", 
requestTrackingId, "Auth User ID Required.", null);
}

if (StringUtils.isNullorEmpty(requestTrackingId)) {

throw new DocException (HttpStatus.BAD_REQUEST, "Request Tracking ID Required.", 
requestTrackingId, "Request Tracking ID Required.", null);
}

if (StringUtils.isNullOrEmpty(bulkUploadGuid)) {

throw new DocException (HttpStatus.BAD_REQUEST, "Bulk Upload GUID Required.",
 requestTrackingId, "Bulk Upload GUID Required.", null);
 }
 
BulkUpload aBulkUpload = bulkUploadRepository.findByBulkUploadGUID(bulkUploadGuid); 
if (null == aBulkUpload)
{
throw new DocException (HttpStatus.NOT_FOUND, "Bulk Upload Guid Not Found", bulkUploadGuid);
}

if (!authUserId.equalsIgnoreCase (aBulkUpload.getCreateUserId())) {

throw new DocException (HttpStatus.FORBIDDEN, "User didn't have access to Publish.", requestTrackingId, 
"User didn't have access to Publish.", null);
}

if (aBulkUpload.isDeleteFl() || aBulkUpload.isPublishFl()) {
throw new DocException (HttpStatus. BAD_REQUEST, "Bulk Upload GUID is either Deleted or Published.", requestTrackingId, 
"Bulk Upload GUID is either Deleted or Published.", null);
}
return aBulkUpload;
}


BulkUploadDocumentRepository.java 


List<BulkUploadDocuments> findAllByBulkUploadId (Long bulkUploadId);

----------------------------------------------------------

UtilService.java

public void pushTOUEAndUpdate (String requestTrackingId, UserEntitlements userEntitlements, List<Document> documents, 
ConcurrentMap<Long, String> success DocumentIds, ConcurrentMap<Long, String> errorDocumentIds) throws InterruptedException { 
try {
      pushToUE (requestTrackingId, userEntitlements, documents, success DocumentIds, errorDocumentIds);
	  } catch (Exception e) {
        logger.error("Error occurred while batch sending document entity updates.", e);
}


*pushTOUE-method to be called when 1 or more documents need to be pushed to UE & Search. 
*@param requestTrackingId      Request Tracking Id - used to track requests coming from caller.
*@param userEntitlements	  User Entitlements object.
*@param documents			  List of documents to push to UE & Search.
*@param successDocumentIds    Map of <documentId, documentGuid> successful documents sent.
*@param errorDocumentIds       Map of <documentId, documentGuid> failed documents sent.
*/
public void pushTOUE (String requestTrackingId, UserEntitlements user Entitlements, List<Document> documents,
ConcurrentMap<Long, String> successDocumentIds, ConcurrentMap<Long, String> errorDocumentIds) {

Date syncDate = new Date();

List<UECreateEntityDocumentRequest> requestList = new ArrayList<>();
for (Document document: documents) {
if (document.getDocumentRelatedEntity() != null) {

try {
requestList.add(documentService.toUECreateEntityDocumentRequest(requestTrackingId, document));
} catch (Exception e) {
}
logger.error("Skipping UE push for documentGuid: {}, missing related entity.", document.getDocumentGuid(), e); 
errorDocumentIds.put(document.getDocumentId(), document.getDocumentGuid());
} 
}else {
logger.info("Skipping UE push for documentGuid: {}, missing related entity.", document.getDocumentGuid()); 
errorDocumentIds.put(document.getDocumentId(), Hocument.getDocumentGuid());
}
}
List<List<UECreateEntityDocumentRequest>> pushListOfLists = CollectionUtils.splitCollection (requestList, 
                                                       propertyConfig.getUserEntitlementCreateEntityMaxCount()); 
													   
	for (List<UECreate EntityDocumentRequest> pushDocuments: pushListOfLists) {
	Map<Long, String> documentIdsToUpdate = pushDocuments.stream().collect (Collectors.toMap (request -> 
	Long.parseLong (request.getAdditionalProperties().getInternalId()), request -> request.getAdditional Properties().getId()));
	try { 
	userEntitlementQueryService.createUE Entity (requestTrackingId, userEntitlements, pushDocuments);
	documentService.updateDocumentsUELastSyncDate (documentIdsToUpdate.keySet(), syncDate); 
	successDocumentIds.putAll(documentIdsToUpdate);
	} catch (Exception e) {
	errorDocumentIds.putAll(documentIdsToUpdate);
	}
	}
	}
	
DocumentService.java and Repository


@Transactional
public int updateDocumentsUELastSyncDate (Collection<Long> documentIds, Date lastSyncDate) {
return documentRepository.updateDocumentsUELastSyncDate (documentIds, lastSyncDate);
}

@Modifying
@Query(value = "UPDATE dbo. Document SET ue_Last_Sync_Dt=: lastSyncDate, ue_syncing_f1-0 WHERE document_id IN :documentId", nativeQuery = true) 
int updateDocumentsUELastSyncDate (@Param("documentId") Collection<Long> documentIds, @Param("lastSyncDate") Date lastSyncDate);

	
	

