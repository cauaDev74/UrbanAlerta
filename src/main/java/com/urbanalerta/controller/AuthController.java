package com.urbanalerta.controller;

import com.urbanalerta.dto.CadastroRequest;
import com.urbanalerta.dto.LoginRequest;
import com.urbanalerta.dto.LoginResponse;
import com.urbanalerta.dto.UsuarioResponse;
import com.urbanalerta.model.Usuario;
import com.urbanalerta.security.AutenticacaoInterceptor;
import com.urbanalerta.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Camada de controle: cadastro, login e dados do usuário logado. */
@RestController
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    /** Público. Cria uma conta de CLIENTE e já devolve o token. */
    @PostMapping("/auth/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse cadastrar(@Valid @RequestBody CadastroRequest req) {
        return LoginResponse.de(auth.cadastrarCliente(req.nome(), req.email(), req.senha()));
    }

    /** Público. Devolve o token JWT, que a tela envia nas próximas chamadas. */
    @PostMapping("/auth/login")
    public LoginResponse entrar(@Valid @RequestBody LoginRequest req) {
        return LoginResponse.de(auth.entrar(req.email(), req.senha()));
    }

    /** Protegido. Dados do usuário dono do token. */
    @GetMapping("/auth/eu")
    public UsuarioResponse eu(@RequestAttribute(AutenticacaoInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
        return UsuarioResponse.de(usuario);
    }
}
