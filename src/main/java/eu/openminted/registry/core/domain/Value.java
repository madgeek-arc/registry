package eu.openminted.registry.core.domain;

public class Value implements Comparable<Value> {
	private String value;
	private String id;
	private int count;

	public Value() {
	}

	public Value(String value, int count) {
		this.value = value;
		this.count = count;
	}

	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}

	public void getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public int compareTo(Value o) {
		return Integer.compare(count, o.count);
	}
}
