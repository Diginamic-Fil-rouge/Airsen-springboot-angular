package fr.airsen.api.mapper;

import fr.airsen.api.dto.request.CreateManualSignalRequest;
import fr.airsen.api.dto.response.AlertSignalDTO;
import fr.airsen.api.entity.AlertSignal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;


@Mapper(componentModel = "spring")
public interface AlertSignalMapper {

    /**
     * Converts AlertSignal entity to DTO.
     *
     * @param entity alert signal entity
     * @return alert signal DTO
     */
    AlertSignalDTO toDTO(AlertSignal entity);

    /**
     * Converts list of AlertSignal entities to DTOs.
     *
     * @param entities list of alert signal entities
     * @return list of alert signal DTOs
     */
    List<AlertSignalDTO> toDTOList(List<AlertSignal> entities);

    /**
     * Converts CreateManualSignalRequest to AlertSignal entity.
     *
     * @param request manual signal creation request
     * @return alert signal entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "detectedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    AlertSignal toEntity(CreateManualSignalRequest request);
}
