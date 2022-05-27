package eu.openminted.registry.core.domain;

import java.util.Objects;

public class Value implements Comparable<Value> {
	private String value;
	private String label;
	private long count;

	public Value() {
	}

	public Value(String value, long count) {
		this.value = value;
		this.count = count;
	}

	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public long getCount() {
		return count;
	}
	public void setCount(long count) {
		this.count = count;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	@Override
	public int compareTo(Value o) {
		return Long.compare(count, o.count);
	}

	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof Value)) return false;
		if (!super.equals(object)) return false;
		Value value1 = (Value) object;
		return count == value1.count && java.util.Objects.equals(value, value1.value) && java.util.Objects.equals(label, value1.label);
	}

	public int hashCode() {
		return Objects.hash(super.hashCode(), value, label, count);
	}
}
