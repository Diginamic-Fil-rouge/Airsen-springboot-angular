package fr.airsen.api.dto.response;

/**
 * Basic commune information for historical data responses.
 * 
 * @param name Commune name
 * @param inseeCode INSEE code (5-digit identifier)
 */
public record CommuneBasicDTO(
    String name,
    String inseeCode
) {}
