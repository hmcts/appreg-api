package uk.gov.hmcts.appregister.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.dto.read.StandardApplicantDto;
import uk.gov.hmcts.appregister.mapper.StandardApplicantMapper;
import uk.gov.hmcts.appregister.model.StandardApplicant;
import uk.gov.hmcts.appregister.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.service.api.StandardApplicantService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StandardApplicationServiceImpl implements StandardApplicantService {

    private final StandardApplicantRepository repository;
    private final StandardApplicantMapper mapper;

    @Override
    public List<StandardApplicantDto> findAll() {
        final List<StandardApplicant> standardApplicants = repository.findAll();

        return standardApplicants.stream()
            .map(mapper::toReadDto)
            .toList();
    }

    @Override
    public StandardApplicantDto findById(Long id) {
        final StandardApplicant standardApplicant = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Standard applicant not found"));

        return mapper.toReadDto(standardApplicant);
    }
}
