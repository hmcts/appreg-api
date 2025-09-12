package uk.gov.hmcts.appregister.common.entity.base;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.math.BigDecimal;
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
    void beforeSave(Object object) {
        log.debug("Saving object of type: {}", object.getClass().getName());
        updateCreatedBy(object);
        updateModifiedBy(object);
        log.debug("Saved object of type: {}", object.getClass().getName());
    }

    @PreUpdate
    void beforeUpdate(Object object) {
        log.debug("Updating object of type: {}", object.getClass().getName());

        updateModifiedBy(object);

        log.debug("Updated object of type: {}", object.getClass().getName());
    }

    void updateCreatedBy(Object object) {
        if (object instanceof Accountable entity) {
            entity.setCreatedUser(userIdentity.getUser());
        }
    }

    void updateModifiedBy(Object object) {
        if (object instanceof Changeable entity) {
            entity.setChangedDate(OffsetDateTime.now(clock));
            entity.setChangedBy(new BigDecimal(userIdentity.getUserNumber()));
        }
    }
}
