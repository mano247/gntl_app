package com.gentlemanstore.notification.mapper;

import com.gentlemanstore.notification.dto.NotificationDTO;
import com.gentlemanstore.notification.model.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    @Mapping(source = "type", target = "type")
    @Mapping(source = "read", target = "isRead")
    NotificationDTO toDTO(Notification notification);
}