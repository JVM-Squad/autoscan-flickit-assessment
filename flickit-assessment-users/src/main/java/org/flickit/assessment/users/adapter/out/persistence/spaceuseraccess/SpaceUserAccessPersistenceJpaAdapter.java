package org.flickit.assessment.users.adapter.out.persistence.spaceuseraccess;

import lombok.RequiredArgsConstructor;
import org.flickit.assessment.common.exception.ResourceNotFoundException;
import org.flickit.assessment.data.jpa.users.space.SpaceJpaRepository;
import org.flickit.assessment.data.jpa.users.spaceuseraccess.SpaceUserAccessJpaEntity;
import org.flickit.assessment.data.jpa.users.spaceuseraccess.SpaceUserAccessJpaRepository;
import org.flickit.assessment.users.application.port.out.space.LoadSpaceOwnerPort;
import org.flickit.assessment.users.application.port.out.spaceuseraccess.AddSpaceMemberPort;
import org.flickit.assessment.users.application.port.out.spaceuseraccess.CheckSpaceAccessPort;
import org.flickit.assessment.users.application.port.out.spaceuseraccess.DeleteSpaceMemberPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.flickit.assessment.users.common.ErrorMessageKey.SPACE_ID_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class SpaceUserAccessPersistenceJpaAdapter implements
    AddSpaceMemberPort,
    CheckSpaceAccessPort,
    LoadSpaceOwnerPort,
    DeleteSpaceMemberPort {

    private final SpaceUserAccessJpaRepository repository;
    private final SpaceJpaRepository spaceRepository;

    @Override
    public void persist(Param param) {
        SpaceUserAccessJpaEntity unsavedEntity = new SpaceUserAccessJpaEntity(param.spaceId(), param.invitee(),
            param.inviter(), param.creationTime());
        repository.save(unsavedEntity);
    }

    @Override
    public boolean checkIsMember(long spaceId, UUID userId) {
        if(!spaceRepository.existsById(spaceId))
            throw new ResourceNotFoundException(SPACE_ID_NOT_FOUND);

        return repository.existsByUserIdAndSpaceId(userId, spaceId);
    }

    @Override
    public UUID loadOwnerId(long id) {
        return repository.loadOwnerIdById(id)
            .orElseThrow(() -> new ResourceNotFoundException(SPACE_ID_NOT_FOUND));
    }

    @Override
    public void delete(long id, UUID userId) {
        repository.deleteBySpaceIdAndUserId(id, userId);
    }
}
