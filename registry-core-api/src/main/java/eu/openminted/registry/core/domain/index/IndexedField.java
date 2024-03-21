package eu.openminted.registry.core.domain.index;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eu.openminted.registry.core.domain.Resource;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

/**
 * Created by antleb on 5/20/16.
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class IndexedField<T extends Object> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JsonIgnore
    private Resource resource;

    @Column
    private String name;

    public IndexedField() {
    }

    public IndexedField(String name, Set<T> values) {
        setName(name);
        setValues(values);
    }

    public abstract Set<T> getValues();

    public abstract void setValues(Set<T> value);

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IndexedField<?> that = (IndexedField<?>) o;

        if (resource != null ? !resource.equals(that.resource) : that.resource != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return getValues() != null ? getValues().equals(this.getValues()) : that.getValues() == null;

    }

    @Override
    public int hashCode() {
        int result = resource != null ? resource.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (getValues() != null ? getValues().hashCode() : 0);
        return result;
    }
}
