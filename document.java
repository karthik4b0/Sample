ublic class Document {

@JoinColumn(name = "Document_Type_rd", updatable = false, insertable = false, nuttable = false) private DocumentType documentType;
@Column(name = "Document_size_Bytes_Vat") private Long sizeInBytes;
@Column(name = "Document_Nm")
private String documentName;
@ValidGuid
@Column(name = "Document_GUID") private String document@uid;
@Validouid
@Column(name = "Alfresco Document_Id") private String alfrescoDocumentId;
@Column(name = "Deleted_FL") private boolean deletedFl;




@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "Document_Id")
private Long documentId;

@OneToOne
@JoinColumn(name = "Document_Path_Id")
private DocumentPath docPathId;

@Column(name = "Document_Type_Id")
private Long docTypeId;

@Column(name = "Type_Cd")
private String docTypeCd;

@ManyToOne
@JoinColumn(name = "Document_MimeType_Id")
 private DocumentMimeTypes documentMimeTypes;
 

@OneToOne
@JoinColumn(name = "Document_Type_rd", updatable = false, insertable = false, nullable = false) 
private DocumentType documentType;

@Column(name = "Document_size_Bytes_Vat") 
private Long sizeInBytes;

@Column(name = "Document_Nm")
private String documentName;

@ValidGuid
@Column(name = "Document_GUID") 
private String document@uid;

@ValidGuid
@Column(name = "Alfresco Document_Id") 
private String alfrescoDocumentId;

@Column(name = "Deleted_FL") 
private boolean deletedFl;




@OneToOne
@JoinColumn(name = "Bulk_Upload_Id", updatable = false, insertable = false)
private BulkUpload bulkUpload;

@Column(name = "Bulk_Upload_Id")
private Long bulkUploadId;

public Document() {
}
public Long getDocumentId() { return documentId; }
public Document setDocumentId (Long documentId) {

this.documentId = documentId;
return this;
}
public DocumentPath getDocPathId() { return docPathId;}
document file.txt
