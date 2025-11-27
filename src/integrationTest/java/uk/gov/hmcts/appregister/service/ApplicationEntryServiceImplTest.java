package uk.gov.hmcts.appregister.service;

import com.nimbusds.jose.Payload;

import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.oauth2.jwt.Jwt;

import uk.gov.hmcts.appregister.applicationentry.service.ApplicationEntryService;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;

public class ApplicationEntryServiceImplTest extends BaseIntegration {

    @Autowired
    private ApplicationEntryService applicationEntryService;

    @Autowired
    private ApplicationListRepository applicationListRepository;

    @BeforeEach
    public void setUp() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(TokenGenerator.builder().build().getJwtFromToken());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    public void createEntryNoRespondent() {
        TransactionalUnitOfWork unitOfWork = new TransactionalUnitOfWork();
        unitOfWork.inTransaction(()-> {
            ApplicationList applicationList = applicationListRepository.findAll().getFirst();

            Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
            EntryCreateDto entryCreateDto =
                Instancio.of(EntryCreateDto.class).withSettings(settings).create();
            entryCreateDto.getApplicant().setOrganisation(null);
            entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");

            entryCreateDto.getRespondent().setOrganisation(null);
            entryCreateDto.setNumberOfRespondents(null);
            entryCreateDto.setRespondent(null);
            entryCreateDto.setApplicationCode("AD99001");
            entryCreateDto.setStandardApplicantCode(null);
            entryCreateDto.setWordingFields(null);

            PayloadForCreate<EntryCreateDto> payloadForCreate = PayloadForCreate.<EntryCreateDto>builder()
                .id(applicationList.getUuid()).data(entryCreateDto).build();
            MatchResponse<EntryGetDetailDto> response = applicationEntryService.createEntry(payloadForCreate);
        });
    }

    @Test
    public void createEntryWithRespondentWithoutFeeDueNoBulkRespondent() {
        TransactionalUnitOfWork unitOfWork = new TransactionalUnitOfWork();
        unitOfWork.inTransaction(()-> {
            ApplicationList applicationList = applicationListRepository.findAll().getFirst();

            Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
            EntryCreateDto entryCreateDto =
                Instancio.of(EntryCreateDto.class).withSettings(settings).create();

            // set the organisation and person applicant to null so we use the standard applicant
            entryCreateDto.getApplicant().setOrganisation(null);
            entryCreateDto.getApplicant().setPerson(null);
            entryCreateDto.setFeeStatuses(null);
            entryCreateDto.getRespondent().setOrganisation(null);
            entryCreateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");
            entryCreateDto.setNumberOfRespondents(0);

            // use the applicant standard applicant
            entryCreateDto.setStandardApplicantCode("APP001");
            entryCreateDto.setNumberOfRespondents(null);
            entryCreateDto.setApplicationCode("CT99002");
            entryCreateDto.setWordingFields(List.of("test wording"));

            PayloadForCreate<EntryCreateDto> payloadForCreate = PayloadForCreate.<EntryCreateDto>builder()
                .id(applicationList.getUuid()).data(entryCreateDto).build();
            MatchResponse<EntryGetDetailDto> response = applicationEntryService.createEntry(payloadForCreate);
        });
    }

    @Test
    public void createEntryWithCodeThatAllowsRespondentBulkRespondentAndFee() {
        TransactionalUnitOfWork unitOfWork = new TransactionalUnitOfWork();
        unitOfWork.inTransaction(()-> {
            ApplicationList applicationList = applicationListRepository.findAll().getFirst();

            Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
            EntryCreateDto entryCreateDto =
                Instancio.of(EntryCreateDto.class).withSettings(settings).create();
            entryCreateDto.getApplicant().setOrganisation(null);
            entryCreateDto.getApplicant().getPerson().getContactDetails().setPostcode("AA1 1AA");
            entryCreateDto.getRespondent().setOrganisation(null);
            entryCreateDto.getRespondent().getPerson().getContactDetails().setPostcode("AA1 1AA");

            entryCreateDto.setNumberOfRespondents(10);

            entryCreateDto.setNumberOfRespondents(null);
            entryCreateDto.setApplicationCode("MS99007");
            entryCreateDto.setStandardApplicantCode(null);

            // fill the template with the two parameters
            entryCreateDto.setWordingFields(List.of("test wording", LocalDate.now().toString()));

            PayloadForCreate<EntryCreateDto> payloadForCreate = PayloadForCreate.<EntryCreateDto>builder()
                .id(applicationList.getUuid()).data(entryCreateDto).build();
            MatchResponse<EntryGetDetailDto> response = applicationEntryService.createEntry(payloadForCreate);
        });
    }
}
