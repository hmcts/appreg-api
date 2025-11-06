package uk.gov.hmcts.appregister.standardapplicant.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.common.concurrency.MatchService;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.model.PayloadForGet;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetDetailDto;
import uk.gov.hmcts.appregister.standardapplicant.dto.StandardApplicantDto;
import uk.gov.hmcts.appregister.standardapplicant.mapper.StandardApplicantMapper;
import uk.gov.hmcts.appregister.standardapplicant.validator.StandardApplicantExistsValidator;

/**
 * Service implementation for managing standard applicants.
 */
@Service
@RequiredArgsConstructor
public class StandardApplicationServiceImpl implements StandardApplicantService {

    private final StandardApplicantRepository repository;
    private final StandardApplicantMapper mapper;
    private final StandardApplicantExistsValidator validator;
    private final MatchService matchService;

    @Override
    @Deprecated
    public List<StandardApplicantDto> findAll() {
        final List<StandardApplicant> standardApplicants = repository.findAll();

        return standardApplicants.stream().map(mapper::toReadDto).toList();
    }

    @Override
    public StandardApplicantGetDetailDto findByCode(String code, LocalDate date) {
        return validator.validate(PayloadForGet.builder().date(date).code(code).build(),
                (id, standardApplicant)
                -> mapper.toReadGetDto(standardApplicant));
    }
}
