package analysis;

import java.io.Serializable;

public class Pair<E,T> implements Serializable {
	
	protected static final long serialVersionUID = -5058336299806637329L;
	protected E first;
	protected T second;
	
	public Pair(E first, T second) {
		this.first = first;
		this.second = second;
	}
	
	public E first() {
		return first;
	}
	
	public T second() {
		return second;
	}
	
	public void first(E first) {
		this.first = first;
	}
	
	public void second(T second) {
		this.second = second;
	}
}