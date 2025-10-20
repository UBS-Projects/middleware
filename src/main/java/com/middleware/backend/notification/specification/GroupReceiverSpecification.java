package com.middleware.backend.notification.specification;

import com.middleware.backend.notification.model.NotificationGroup;
import com.middleware.backend.notification.model.Receiver;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class GroupReceiverSpecification {

    // Filter by group name
    public static Specification<NotificationGroup> hasGroupName(String groupName) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(groupName)) return cb.conjunction();
            return cb.like(cb.lower(root.get("name")), "%" + groupName.toLowerCase() + "%");
        };
    }

    // Filter by receiver name
    public static Specification<NotificationGroup> hasReceiverName(String receiverName) {
        return (root, query, cb) -> {
            Join<NotificationGroup, Receiver> receiversJoin = root.join("receivers", JoinType.INNER);
            query.distinct(true);

            if (!StringUtils.hasText(receiverName)) {
                return cb.conjunction(); // no additional filter, but INNER JOIN ensures at least one receiver
            }

            return cb.like(cb.lower(receiversJoin.get("name")), "%" + receiverName.toLowerCase() + "%");
        };
    }

    // Combined specification
    public static Specification<NotificationGroup> filterBy(String groupName, String receiverName) {
        return Specification.where(hasGroupName(groupName))
                .and(hasReceiverName(receiverName));
    }
}
