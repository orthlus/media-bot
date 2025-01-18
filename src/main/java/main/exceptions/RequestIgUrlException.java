package main.exceptions;

public class RequestIgUrlException extends RuntimeException {
	public RequestIgUrlException(String s) {
		super(s);
	}

	public RequestIgUrlException(String s, RuntimeException e) {
		super(s, e);
	}
}
