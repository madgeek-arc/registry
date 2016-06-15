package eu.openminted.registry.core.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Size;

@Entity
@Table(name="SchemaDatabase")
public class Schema {
	
	@Id
	@Size(min=3, max=400)
    @Column(name = "id", nullable = false)
	private String id;
	
	@Size(max=100000)
    @Column(name = "schema", nullable = false)
	private String schema;
	
	@Size(max=40000)
	@Column(name="originalURL")
	private String originalUrl;
	

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getOriginalUrl() {
		return originalUrl;
	}

	public void setOriginalUrl(String originalUrl) {
		this.originalUrl = originalUrl;
	}
	

}
