package br.edu.ufersa.pw.focustask.features.user;

import br.edu.ufersa.pw.focustask.shared.exception.NegocioException;

public class EmailAlreadyExistsException extends NegocioException {
    public EmailAlreadyExistsException() { super("Email already registered"); }
}
