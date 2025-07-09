package com.milosz.podsiadly.common.mapper;


import org.mapstruct.IterableMapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * Bazowy interfejs dla maperów MapStruct.
 * Zawiera generyczne metody do mapowania pomiędzy DTO a encjami.
 * Dzięki temu, inne maperzy mogą dziedziczyć te podstawowe funkcje.
 *
 * Ważne: MapStruct generuje implementacje tych interfejsów podczas kompilacji.
 * Aby MapStruct działał poprawnie, musisz dodać go jako procesor adnotacji w build.gradle.
 */
public interface BaseMapper<E, D> { // E - Entity, D - DTO

    @Named("toDto")
    D toDto(E entity);

    @Named("toEntity")
    E toEntity(D dto);

    @IterableMapping(qualifiedByName = "toDto")
    List<D> toDtoList(List<E> entityList);

    @IterableMapping(qualifiedByName = "toEntity")
    List<E> toEntityList(List<D> dtoList);
}