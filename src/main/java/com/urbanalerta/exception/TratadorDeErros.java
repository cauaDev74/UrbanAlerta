package com.urbanalerta.exception;

import com.urbanalerta.dto.ErroResponse;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Traduz as exceções do sistema em respostas HTTP com uma mensagem clara em português. */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler({NaoAutenticadoException.class, CredenciaisInvalidasException.class})
    public ResponseEntity<ErroResponse> naoAutenticado(RuntimeException e) {
        return resposta(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResponse> acessoNegado(AcessoNegadoException e) {
        return resposta(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(OcorrenciaNaoEncontradaException.class)
    public ResponseEntity<ErroResponse> naoEncontrada(OcorrenciaNaoEncontradaException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({TransicaoInvalidaException.class, EmailJaCadastradoException.class})
    public ResponseEntity<ErroResponse> conflito(RuntimeException e) {
        return resposta(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(EnderecoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> enderecoNaoEncontrado(EnderecoNaoEncontradoException e) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(GeocodificacaoIndisponivelException.class)
    public ResponseEntity<ErroResponse> geocodificacaoIndisponivel(GeocodificacaoIndisponivelException e) {
        return resposta(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException e) {
        FieldError primeiro = e.getBindingResult().getFieldErrors().get(0);
        return resposta(HttpStatus.BAD_REQUEST, primeiro.getDefaultMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException e) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido. Verifique o tipo, o status e o formato da data (ex.: 2026-10-14T18:30:00).");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> identificadorInvalido(MethodArgumentTypeMismatchException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Identificador inválido.");
    }

    /** Duas pessoas alteraram a mesma ocorrência ao mesmo tempo (lock otimista do Hibernate). */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErroResponse> conflitoDeVersao(OptimisticLockingFailureException e) {
        return resposta(HttpStatus.CONFLICT,
                "Esta ocorrência foi alterada por outra pessoa. Atualize a tela e tente novamente.");
    }

    private ResponseEntity<ErroResponse> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(new ErroResponse(mensagem));
    }
}
