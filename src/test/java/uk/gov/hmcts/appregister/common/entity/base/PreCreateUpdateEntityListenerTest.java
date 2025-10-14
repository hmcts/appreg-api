package uk.gov.hmcts.appregister.common.entity.base;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.common.entity.ApplicationRegister;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.data.ApplicationRegisterTestData;

@ExtendWith(MockitoExtension.class)
class PreCreateUpdateEntityListenerTest {

    @Mock private UserProvider userProvider;

    @Mock private Clock clock;

    @InjectMocks private PreCreateUpdateEntityListener listener;

    @Test
    void testCreate() {
        String userName = "test-user";
        String email = "email";
        when(clock.instant()).thenReturn(java.time.Instant.parse("2024-01-01T10:00:00Z"));
        when(clock.getZone()).thenReturn(java.time.ZoneId.of("UTC"));
        when(userProvider.getUserId()).thenReturn(userName);
        when(userProvider.getEmail()).thenReturn(email);

        ApplicationRegister register = new ApplicationRegisterTestData().someMinimal().build();
        listener.beforeSave(register);
        Assertions.assertEquals(email, register.getCreatedUser());
        Assertions.assertEquals(
                OffsetDateTime.parse("2024-01-01T10:00:00Z"), register.getChangedDate());
        Assertions.assertEquals(userName, register.getChangedBy());
    }

    @Test
    void testUpdate() {
        String userName = "test-user";
        String email = "email";
        String email1 = "email1";

        when(clock.instant()).thenReturn(java.time.Instant.parse("2024-01-01T10:00:00Z"));
        when(clock.getZone()).thenReturn(java.time.ZoneId.of("UTC"));
        when(userProvider.getUserId()).thenReturn(userName);
        when(userProvider.getEmail()).thenReturn(email);
        ApplicationRegister register = new ApplicationRegisterTestData().someMinimal().build();

        // create the entity
        listener.beforeSave(register);

        // setup a new id to use for the update
        when(clock.instant()).thenReturn(java.time.Instant.parse("2024-03-01T10:00:00Z"));

        // run the test
        listener.beforeUpdate(register);

        // assert that the update has occurred on the relevant fields but it does not adversely
        // effect the created fields
        Assertions.assertEquals(email, register.getCreatedUser());
        Assertions.assertEquals(
                OffsetDateTime.parse("2024-03-01T10:00:00Z"), register.getChangedDate());
        Assertions.assertEquals(userName, register.getChangedBy());

        // verify that the user provider was called twice, once for create
        verify(userProvider, times(2)).getUserId();
        verify(userProvider, times(1)).getEmail();
    }
}
