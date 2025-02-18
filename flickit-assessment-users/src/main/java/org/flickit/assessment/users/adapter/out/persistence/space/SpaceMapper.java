package org.flickit.assessment.users.adapter.out.persistence.space;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.flickit.assessment.data.jpa.users.space.SpaceJpaEntity;
import org.flickit.assessment.users.application.domain.Space;
import org.flickit.assessment.common.application.domain.space.SpaceType;

import java.util.Objects;

import static org.flickit.assessment.users.application.service.constant.SpaceConstants.NOT_DELETED_DELETION_TIME;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SpaceMapper {

    public static SpaceJpaEntity mapToJpaEntity(Space space) {
        return new SpaceJpaEntity(
            null,
            space.getCode(),
            space.getTitle(),
            SpaceType.valueOf(space.getType().getCode()).getId(),
            space.getOwnerId(),
            space.getSubscriptionExpiry(),
            space.getCreationTime(),
            space.getLastModificationTime(),
            space.getCreatedBy(),
            space.getLastModifiedBy(),
            false,
            NOT_DELETED_DELETION_TIME);
    }

    public static Space mapToDomain(SpaceJpaEntity entity) {
        var space = SpaceType.valueOfById(entity.getType());
        return new Space(
            entity.getId(),
            entity.getCode(),
            entity.getTitle(),
            new Space.SpaceType(Objects.requireNonNull(space).getCode(), space.getTitle()),
            entity.getOwnerId(),
            entity.getSubscriptionExpiry(),
            entity.getCreationTime(),
            entity.getLastModificationTime(),
            entity.getCreatedBy(),
            entity.getLastModifiedBy());
    }
}
