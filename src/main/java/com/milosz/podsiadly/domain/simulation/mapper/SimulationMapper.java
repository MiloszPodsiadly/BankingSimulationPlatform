package com.milosz.podsiadly.domain.simulation.mapper;

import com.milosz.podsiadly.domain.simulation.dto.ScenarioEventDto;
import com.milosz.podsiadly.domain.simulation.dto.SimulationConfigDto;
import com.milosz.podsiadly.domain.simulation.dto.SimulationRunStatusDto;
import com.milosz.podsiadly.domain.simulation.model.ScenarioEvent;
import com.milosz.podsiadly.domain.simulation.model.SimulationRun;
import com.milosz.podsiadly.domain.simulation.model.SimulationScenario;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers; // Dodaj ten import

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface SimulationMapper {

    // --- SimulationScenario Mappings ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SimulationScenario toSimulationScenarioEntity(SimulationConfigDto dto);

    @Mapping(target = "scenarioName", source = "scenarioName")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "scenarioType", source = "scenarioType")
    @Mapping(target = "startDate", source = "startDate")
    @Mapping(target = "endDate", source = "endDate")
    @Mapping(target = "durationInDays", source = "durationInDays")
    @Mapping(target = "parameters", source = "parameters")
    SimulationConfigDto toSimulationConfigDto(SimulationScenario entity);

    List<SimulationConfigDto> toSimulationConfigDtoList(List<SimulationScenario> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateSimulationScenarioFromDto(SimulationConfigDto dto, @MappingTarget SimulationScenario entity);


    // --- SimulationRun Mappings ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "runIdentifier", ignore = true)
    @Mapping(target = "startTime", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "simulationScenario", source = "scenario")
    SimulationRun toSimulationRunEntity(SimulationScenario scenario);

    @Mapping(target = "scenarioId", source = "simulationScenario.id")
    @Mapping(target = "scenarioName", source = "simulationScenario.scenarioName")
    SimulationRunStatusDto toSimulationRunStatusDto(SimulationRun entity);

    List<SimulationRunStatusDto> toSimulationRunStatusDtoList(List<SimulationRun> entities);


    // --- ScenarioEvent Mappings ---

    // Metoda do mapowania ScenarioEventDto na ScenarioEvent, gdy obiekt SimulationRun jest dostarczany
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventTimestamp", ignore = true) // Handled by @PrePersist in ScenarioEvent
    // MapStruct automatycznie zmapuje obiekt SimulationRun (z @Context) do pola simulationRun w encji.
    // Dzieje się tak, ponieważ nazwy są zgodne i typy są zgodne (SimulationRun -> SimulationRun).
    // Jeśli potrzebowałbyś mapować ID z SimulationRun na simulationRunId w DTO, to poniższa adnotacja byłaby zbędna
    // i trzeba by było dodać inną metodę toDto, która mapuje simulationRun.id na simulationRunId
    // Poniższa adnotacja NIE JEST POTRZEBNA, JEŚLI TYPY SĄ ZGODNE I NAZWY PÓL SĄ ZGODNE:
    // @Mapping(source = "simulationRun", target = "simulationRun") // <-- Niepotrzebne, MapStruct to zrobi sam
    ScenarioEvent toScenarioEventEntity(ScenarioEventDto dto, @Context SimulationRun simulationRun);


    // Konwertuj DTO na encję (użyteczne, jeśli tworzysz zdarzenia bezpośrednio przez API bez pełnego obiektu run)
    // Ważne: W tym przypadku simulationRun będzie null po mapowaniu i BĘDZIE MUSIAŁO BYĆ USTAWIENIE RĘCZNIE W SERWISIE!
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventTimestamp", ignore = true) // Generated in @PrePersist
    @Mapping(target = "simulationRun", ignore = true) // BARDZO WAŻNE: Tutaj IGNORUJEMY, będzie ustawione w serwisie
    ScenarioEvent toScenarioEventEntity(ScenarioEventDto dto); // Simplified mapping for creating new event

    // Mapowanie z encji ScenarioEvent na DTO ScenarioEventDto
    // Tutaj musimy zmapować obiekt SimulationRun na simulationRunId
    @Mapping(source = "simulationRun.id", target = "simulationRunId") // Mapuje ID z SimulationRun do simulationRunId w DTO
    ScenarioEventDto toScenarioEventDto(ScenarioEvent entity);

    List<ScenarioEventDto> toScenarioEventDtoList(List<ScenarioEvent> entities);

}