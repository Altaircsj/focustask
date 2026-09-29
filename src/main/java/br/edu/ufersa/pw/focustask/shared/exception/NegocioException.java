package br.edu.ufersa.pw.focustask.shared.exception;

public abstract class NegocioException extends RuntimeException {
    protected NegocioException(String message) { super(message); }
}
