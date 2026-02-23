package uk.gov.hmcts.appregister.service;

import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.appregister.applicationlist.service.ApplicationListService;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.model.PayloadForUpdate;
import uk.gov.hmcts.appregister.controller.applicationlist.AbstractApplicationListTest;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.ApplicationListUpdateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.HeaderUtil;

public class ApplicationListServiceImplTest extends AbstractApplicationListTest {
    @Autowired private ApplicationListService applicationListService;

    @Autowired private TransactionalUnitOfWork unitOfWork;

    @Autowired private ApplicationListRepository applicationListRepository;

    @BeforeEach
    public void setUp() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal())
                .thenReturn(TokenGenerator.builder().build().getJwtFromToken());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    public void updateAppListToClosed() throws Exception {
        String[] createdLocation = createAppListUsingRestApi();

        // create an entry
        EntryGetDetailDto entryGetSummaryDto =
                createEntryForClose(
                        UUID.fromString(HeaderUtil.getTrailingIdFromLocation(createdLocation[0])));

        // create the result for the entry
        createResultSuccess(entryGetSummaryDto.getListId(), entryGetSummaryDto.getId());

        unitOfWork.inTransaction(
                () -> {
                    Optional<ApplicationList> applicationList =
                            applicationListRepository.findByUuid(
                                    UUID.fromString(
                                            HeaderUtil.getTrailingIdFromLocation(
                                                    createdLocation[0])));

                    // update the app list with new values
                    ApplicationListUpdateDto applicationListUpdateDto =
                            new ApplicationListUpdateDto();
                    applicationListUpdateDto.setStatus(ApplicationListStatus.CLOSED);
                    applicationListUpdateDto.setDurationHours(1);
                    applicationListUpdateDto.setDurationMinutes(22);
                    applicationListUpdateDto.setDescription("test update");
                    applicationListUpdateDto.setCourtLocationCode(
                            applicationList.get().getCourtCode());
                    applicationListUpdateDto.setDate(applicationList.get().getDate());
                    applicationListUpdateDto.setTime(applicationList.get().getTime());

                    PayloadForUpdate<ApplicationListUpdateDto> payload =
                            PayloadForUpdate.<ApplicationListUpdateDto>builder()
                                    .id(
                                            UUID.fromString(
                                                    HeaderUtil.getTrailingIdFromLocation(
                                                            createdLocation[0])))
                                    .data(applicationListUpdateDto)
                                    .build();
                    applicationListService.update(payload);

                    // assert the app list is updated with the new values
                    applicationList =
                            applicationListRepository.findByUuid(
                                    UUID.fromString(
                                            HeaderUtil.getTrailingIdFromLocation(
                                                    createdLocation[0])));
                    Assertions.assertEquals(Status.CLOSED, applicationList.get().getStatus());
                    Assertions.assertEquals("test update", applicationList.get().getDescription());
                    Assertions.assertEquals(1, applicationList.get().getDurationHours());
                    Assertions.assertEquals(22, applicationList.get().getDurationMinutes());
                });
    }
}
