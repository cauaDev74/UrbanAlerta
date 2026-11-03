package com.urbanalerta.security;

import com.urbanalerta.model.PerfilUsuario;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marca um endpoint que só pode ser usado por um perfil específico (ex.: só o ADMIN muda o status). */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExigePerfil {
    PerfilUsuario value();
}
