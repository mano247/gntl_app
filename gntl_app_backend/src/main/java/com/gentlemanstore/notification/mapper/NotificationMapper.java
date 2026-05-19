package com.gentlemanstore.notification.mapper;

import com.gentlemanstore.notification.dto.NotificationDTO;
import com.gentlemanstore.notification.model.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    @Mapping(source = "notificationType", target = "notificationType")
    NotificationDTO toDTO(Notification notification);
}
