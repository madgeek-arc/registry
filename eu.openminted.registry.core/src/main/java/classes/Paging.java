package classes;

import java.util.List;

public class Paging {
	 int total;
	 int from;
	 int to;
	 List<?> results;

	 public Paging(int total, int from, int to, List<?> results) {
		 this.total = total;
		 this.from = from;
		 this.to = to;
		 this.results = results;
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
}
