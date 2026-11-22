package br.edu.ufersa.pw.focustask.features.user;

public class IncorrectCurrentPasswordException extends RuntimeException {
    public IncorrectCurrentPasswordException() { super("Senha atual incorreta."); }
}
