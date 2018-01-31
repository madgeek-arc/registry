package eu.openminted.registry.core.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.Date;

@Entity
@Table(name="ResourceVersion")
public class Version {

	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="parent_id", nullable = false)
	@JsonIgnore
	private Resource resource;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="fk_name_version", nullable = false)
	@JsonIgnore
	@JsonBackReference
	private ResourceType resourceType;

	@Size(min=3, max=50)
	@Column(name = "version", nullable = false)
	private String version;

	@Column(name = "payload", nullable = false, columnDefinition = "text")
	private String payload;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "creation_date", nullable = false, updatable=false)
	private Date creationDate;

	public Version(String id, Resource resource, ResourceType resourceType, String version, String payload, Date creationDate) {
		this.id = id;
		this.resource = resource;
		this.resourceType = resourceType;
		this.version = version;
		this.payload = payload;
	}

	public Version(){

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

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	@PrePersist
	protected void onCreate() {
		creationDate = new Date();
	}

	public Resource getResource() {
		return resource;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}
}
