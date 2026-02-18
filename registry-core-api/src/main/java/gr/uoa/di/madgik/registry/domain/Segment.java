package gr.uoa.di.madgik.registry.domain;

import java.util.List;

public class Segment {

    String label;
    float weight;
    List<String> values;

    public Segment() {
    }

    public Segment(String label, float weight, List<String> values) {
        this.label = label;
        this.weight = weight;
        this.values = values;
    }

    public Segment(String label, float weight, String... values) {
        this.label = label;
        this.weight = weight;
        this.values = List.of(values);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }
}
