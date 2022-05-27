package eu.openminted.registry.core.domain;

import java.util.List;
import java.util.Objects;

public class Facet {

	 private String field;
	 private String label;
	 private List<Value> values;

	public Facet() {
	}

	public Facet(String field, String label, List<Value> values) {
		this.field = field;
		this.label = label;
		this.values = values;
	}

	public String getField() {
		return field;
	}
	public void setField(String field) {
		this.field = field;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public List<Value> getValues() {
		return values;
	}
	public void setValues(List<Value> values) {
		this.values = values;
	}

	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof Facet)) return false;
		if (!super.equals(object)) return false;
		Facet facet = (Facet) object;
		return java.util.Objects.equals(field, facet.field) && java.util.Objects.equals(label, facet.label) && java.util.Objects.equals(values, facet.values);
	}

	public int hashCode() {
		return Objects.hash(super.hashCode(), field, label, values);
	}
}
