package eu.openminted.registry.core.domain;

import java.util.List;

public class Paging {
	 int total;
	 int from;
	 int to;
	 List<?> results;
	 List<?> occurencies;

	 public Paging(int total, int from, int to, List<?> results, List<?> occurencies) {
		 this.total = total;
		 this.from = from;
		 this.to = to;
		 this.results = results;
		 this.occurencies = occurencies;
	 }
	 
	 public int getTotal() {
		return total;
	}
	public void setTotal(int total) {
		this.total = total;
	}
	public int getFrom() {
		return from;
	}
	public void setFrom(int from) {
		this.from = from;
	}
	public int getTo() {
		return to;
	}
	public void setTo(int to) {
		this.to = to;
	}
	public List<?> getResults() {
		return results;
	}
	public void setResults(List<?> results) {
		this.results = results;
	}

	public List<?> getOccurencies() {
		return occurencies;
	}

	public void setOccurencies(List<?> occurencies) {
		this.occurencies = occurencies;
	}
	
}
