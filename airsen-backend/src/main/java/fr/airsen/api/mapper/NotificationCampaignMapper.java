package fr.airsen.api.mapper;

import fr.airsen.api.dto.request.CreateCampaignRequest;
import fr.airsen.api.dto.response.NotificationCampaignDTO;
import fr.airsen.api.entity.NotificationCampaign;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;


@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface NotificationCampaignMapper {

    @Mapping(target = "deliveryRate", expression = "java(entity.getDeliveryRate())")
    NotificationCampaignDTO toDTO(NotificationCampaign entity);

    List<NotificationCampaignDTO> toDTOList(List<NotificationCampaign> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalRecipients", ignore = true)
    @Mapping(target = "sentCount", ignore = true)
    @Mapping(target = "failedCount", ignore = true)
    @Mapping(target = "notifications", ignore = true)
    @Mapping(target = "alertSignal", ignore = true)
    NotificationCampaign toEntity(CreateCampaignRequest request);
}
