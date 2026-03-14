package io.attestry.commonlib.infrastructure;

import java.util.List;

public interface DomainMapper<D, E>  {


    D toDomain(E entity);

    E toEntity(D domain);

    default List<D> toDomainList(List<E> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDomain)
                .toList();
    }


    default List<E> toEntityList(List<D> domains) {
        if (domains == null) {
            return List.of();
        }
        return domains.stream()
                .map(this::toEntity)
                .toList();
    }
}
