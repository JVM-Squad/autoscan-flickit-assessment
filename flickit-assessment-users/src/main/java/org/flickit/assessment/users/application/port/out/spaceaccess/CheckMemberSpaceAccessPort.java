package org.flickit.assessment.users.application.port.out.spaceaccess;

import java.util.UUID;
public interface CheckMemberSpaceAccessPort {

    boolean checkAccess(UUID userId);
}
