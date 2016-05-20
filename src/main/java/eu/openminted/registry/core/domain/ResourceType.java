package domain;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.Size;

@Entity
@Table(name="ResourceType")
public class ResourceType {
	
	@Id
	@Column(name = "id", nullable = false)
	private String id;	
	
	@Size(min=3, max=50)
    @Column(name = "name", nullable = false)
	private String name;
	
	@Size(min=3, max=10000)
    @Column(name = "schema", nullable = false)
	private String schema;
	
	@Size(min=3, max=200)
    @Column(name = "schemaUrl", nullable = false)
	private String schemaUrl;
	
	@Size(min=3, max=30)
    @Column(name = "payloadType", nullable = false)
	private String payloadType;
	
	@Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_date", nullable = false , updatable=false)
	private Date creationDate;
	
	@Temporal(TemporalType.TIMESTAMP)
    @Column(name = "modification_date", nullable = false)
	private Date modificationDate;
	
	
	
	public ResourceType(){
		
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String string) {
		this.id = string;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getPayloadType() {
		return payloadType;
	}

	public void setPayloadType(String payloadType) {
		this.payloadType = payloadType;
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

	public String getSchemaUrl() {
		return schemaUrl;
	}

	public void setSchemaUrl(String schemaUrl) {
		this.schemaUrl = schemaUrl;
	}

	@PrePersist
	private void ensureId(){
	    this.setId(UUID.randomUUID().toString());
	}
	
	@PrePersist
	protected void onCreate() {
		modificationDate = creationDate = new Date();
	}

	@PreUpdate
	protected void onUpdate() {
	   modificationDate = new Date();
	}


}
