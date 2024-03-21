package gr.uoa.di.madgik.registry.domain;

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
}
