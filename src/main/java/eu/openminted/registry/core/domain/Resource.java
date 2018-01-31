package eu.openminted.registry.core.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import eu.openminted.registry.core.domain.index.IndexedField;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Entity
@Table(name="Resource")
public class Resource {

	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="fk_name", nullable = false)
	@JsonManagedReference
	private ResourceType resourceType;

	@Size(min=3, max=50)
	@Column(name = "version", nullable = false)
	private String version;

	@Column(name = "payload", nullable = false, columnDefinition = "text")
	private String payload;

	@Transient
	private String payloadUrl;

	@Transient
	private String searchableArea;

	@Size(min=3, max=30)
	@Column(name = "payloadFormat", nullable = false)
	private String payloadFormat;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "creation_date", nullable = false, updatable=false)
	private Date creationDate;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "modification_date", nullable = false)
	private Date modificationDate;

	@OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.ALL}, mappedBy = "resource")
	@JsonManagedReference
	private List<IndexedField> indexedFields;

	@OneToMany(mappedBy = "resource", fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
	private List<Version> versions;


	public Resource(String id, ResourceType resourceType,String version,String payload,String payloadFormat) {
		this.id = id;
		this.resourceType = resourceType;
		this.version = version;
		this.payload = payload;
		this.payloadFormat = payloadFormat;
	}

	public Resource(){

	}

	public String getId() {
		return id;
	}

	public void setId(String string) {
		this.id = string;
	}

	public ResourceType getResourceType() {
		return resourceType;
	}

	public void setResourceType(ResourceType resourceType) {
		this.resourceType = resourceType;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public String getPayloadFormat() {
		return payloadFormat;
	}

	public void setPayloadFormat(String payloadFormat) {
		this.payloadFormat = payloadFormat;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public Date getModificationDate() {
		return modificationDate;
	}

	public void setModificationDate(Date modificationDate) {
		this.modificationDate = modificationDate;
	}

	public String getPayloadUrl() {
		return payloadUrl;
	}

	public void setPayloadUrl(String payloadUrl) {
		this.payloadUrl = payloadUrl;
	}

	public List<IndexedField> getIndexedFields() {
		return indexedFields;
	}

	public void setIndexedFields(List<IndexedField> indexedFields) {
		this.indexedFields = indexedFields;
	}

	public String getSearchableArea() {
		return searchableArea;
	}

	public void setSearchableArea(String searchableArea) {
		this.searchableArea = searchableArea;
	}

	@PrePersist
	protected void onCreate() {
		modificationDate = creationDate = new Date();
		version = generateVersion();
	}

	@PreUpdate
	protected void onUpdate() {

		modificationDate = new Date();
		version = generateVersion();
	}

	public List<Version> getVersions() {
		return versions;
	}

	public void setVersions(List<Version> versions) {
		this.versions = versions;
	}

	private String generateVersion(){
		DateFormat df = new SimpleDateFormat("MMddyyyyHHmmss");
		return df.format(Calendar.getInstance().getTime());
	}
}
