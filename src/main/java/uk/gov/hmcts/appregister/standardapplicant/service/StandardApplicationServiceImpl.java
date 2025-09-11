package uk.gov.hmcts.appregister.standardapplicant.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.standardapplicant.dto.StandardApplicantDto;
import uk.gov.hmcts.appregister.standardapplicant.mapper.StandardApplicantMapper;

/** Service implementation for managing standard applicants. */
@Service
@RequiredArgsConstructor
public class StandardApplicationServiceImpl implements StandardApplicantService {

    private final StandardApplicantRepository repository;
    private final StandardApplicantMapper mapper;

    @Override
    public List<StandardApplicantDto> findAll() {
        final List<StandardApplicant> standardApplicants = repository.findAll();

        return standardApplicants.stream().map(mapper::toReadDto).toList();
    }

    @Override
    public StandardApplicantDto findById(Long id) {
        final StandardApplicant standardApplicant =
                repository
                    .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Standard applicant not found"));

        return mapper.toReadDto(standardApplicant);
    }
}
