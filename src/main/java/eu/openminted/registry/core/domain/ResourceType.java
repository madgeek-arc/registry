package eu.openminted.registry.core.domain;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import eu.openminted.registry.core.domain.index.IndexField;

@Entity
@Table(name="ResourceType")
//@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ResourceType {
	

	@Id
	@Size(min=3, max=50)
    @Column(name = "name", nullable = false)
	private String name;
	
	@Size(min=3, max=10000)
    @Column(name = "schema", nullable = false)
	private String schema;
	
	@Transient
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

	@Column
	private String indexMapperClass;

	@OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
//	@ElementCollection(targetClass = IndexField.class)
	@Column
	@JsonManagedReference
	private List<IndexField> indexFields;
	
	public ResourceType(){
		
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

	public String getIndexMapperClass() {
		return indexMapperClass;
	}

	public void setIndexMapperClass(String indexMapperClass) {
		this.indexMapperClass = indexMapperClass;
	}

	public List<IndexField> getIndexFields() {
		return indexFields;
	}

	public void setIndexFields(List<IndexField> indexFields) {
		this.indexFields = indexFields;
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
