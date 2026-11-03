package com.urbanalerta.support;

import com.urbanalerta.model.Ocorrencia;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Repositório "de mentira" para os testes: guarda tudo em um mapa, sem banco de dados.
 * Cria, em tempo de execução, uma implementação de qualquer interface de repositório do projeto
 * (OcorrenciaRepository, UsuarioRepository) que responde aos métodos usados pelos serviços.
 */
public final class RepositorioFalso {

    private RepositorioFalso() {
    }

    public static <T> T de(Class<T> interfaceRepositorio) {
        Map<Object, Object> dados = new LinkedHashMap<>();
        InvocationHandler tratador = (proxy, metodo, args) -> switch (metodo.getName()) {
            case "save" -> {
                dados.put(ler(args[0], "getId"), args[0]);
                yield args[0];
            }
            case "findById" -> Optional.ofNullable(dados.get(args[0]));
            case "count" -> (long) dados.size();
            case "findAllByOrderByDataHoraRegistroDesc" -> {
                var lista = new ArrayList<>(dados.values());
                lista.sort(Comparator.comparing((Object o) -> ((Ocorrencia) o).getDataHoraRegistro()).reversed());
                yield lista;
            }
            case "findByEmail" -> dados.values().stream().filter(u -> ler(u, "getEmail").equals(args[0])).findFirst();
            case "existsByEmail" -> dados.values().stream().anyMatch(u -> ler(u, "getEmail").equals(args[0]));
            case "toString" -> "RepositorioFalso";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new UnsupportedOperationException("RepositorioFalso não implementa " + metodo.getName());
        };
        return interfaceRepositorio.cast(Proxy.newProxyInstance(
                interfaceRepositorio.getClassLoader(), new Class<?>[]{interfaceRepositorio}, tratador));
    }

    private static Object ler(Object objeto, String getter) {
        try {
            Method metodo = objeto.getClass().getMethod(getter);
            return metodo.invoke(objeto);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
