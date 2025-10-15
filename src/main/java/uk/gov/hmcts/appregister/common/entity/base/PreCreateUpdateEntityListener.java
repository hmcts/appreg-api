package uk.gov.hmcts.appregister.common.entity.base;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Clock;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.security.UserProvider;

/**
 * A database entity listener that updates standard fields when an object is saved or updated
 * Changed by, Changed Date, Version, Username.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PreCreateUpdateEntityListener {

    /** The logged in user. */
    private final UserProvider userIdentity;

    private final Clock clock;

    @PrePersist
    public void beforeSave(Object object) {
        log.debug("Saving object of type: {}", object.getClass().getName());
        updateCreatedBy(object);
        updateModifiedBy(object);
        log.debug("Saved object of type: {}", object.getClass().getName());
    }

    @PreUpdate
    public void beforeUpdate(Object object) {
        log.debug("Updating object of type: {}", object.getClass().getName());

        updateModifiedBy(object);

        log.debug("Updated object of type: {}", object.getClass().getName());

        // if the record is set to be deleted then set the deleted date and deleted by fields
        if (object instanceof Deletable deletable) {
            // ensure that we only change the deleted date once
            if (deletable.isDeleted() && deletable.getDeletedDate() == null) {
                deletable.setDeletedDate(OffsetDateTime.now(clock));
                deletable.setDeletedBy(userIdentity.getUserId());
            }
        }
    }

    void updateCreatedBy(Object object) {
        if (object instanceof Accountable entity) {
            entity.setCreatedUser(userIdentity.getEmail());
        }
    }

    void updateModifiedBy(Object object) {
        if (object instanceof Changeable entity) {
            entity.setChangedDate(OffsetDateTime.now(clock));
            entity.setChangedBy(userIdentity.getUserId());
        }
    }
}
