package com.urbanalerta.repository;

import com.urbanalerta.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /** O e-mail é sempre guardado e buscado em minúsculas. */
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
}
