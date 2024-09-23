package gr.uoa.di.madgik.registry.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "SchemaDatabase")
public class Schema {

    @Id
    @Size(min = 3, max = 400)
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "schema", nullable = false, columnDefinition = "text")
    private String schema;

    @Column(name = "originalurl", length = 1000)
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


    public String toString() {
        return "[" + id + ", " + originalUrl + "]";
    }
}
