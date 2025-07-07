package uk.gov.hmcts.appregister.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.dto.read.ApplicationListDto;
import uk.gov.hmcts.appregister.dto.write.ApplicationListWriteDto;
import uk.gov.hmcts.appregister.mapper.ApplicationListMapper;
import uk.gov.hmcts.appregister.model.ApplicationList;
import uk.gov.hmcts.appregister.model.CourtHouse;
import uk.gov.hmcts.appregister.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.repository.CourtHouseRepository;
import uk.gov.hmcts.appregister.service.api.ApplicationListService;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ApplicationListServiceImpl implements ApplicationListService {

    private final ApplicationListRepository repository;
    private final ApplicationListMapper mapper;
    private final VersionManager versionManager;
    private final CourtHouseRepository courtHouseRepository;

    @Override
    public List<ApplicationListDto> getAllForUser(String userId) {
        return repository.findAllByUserId(userId)
            .stream()
            .map(mapper::toReadDto)
            .toList();
    }

    @Override
    public ApplicationListDto getByIdForUser(Long id, String userId) {
        return repository.findByIdAndUserId(id, userId)
            .map(mapper::toReadDto)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application list not found"));
    }

    @Override
    @Transactional
    public ApplicationListDto create(ApplicationListWriteDto dto, String userId) {
        CourtHouse courthouse = courtHouseRepository.findById(dto.courthouseId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Courthouse not found"));

        ApplicationList entity = mapper.createEntityFromWriteDto(dto, userId, LocalDate.now(), courthouse);
        entity.setVersion(versionManager.increment(entity.getVersion()));

        return mapper.toReadDto(repository.save(entity));
    }

    @Override
    @Transactional
    public ApplicationListDto update(Long id, ApplicationListWriteDto dto, String userId) {
        ApplicationList existing = repository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application list not found"));

        CourtHouse courthouse = courtHouseRepository.findById(dto.courthouseId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Courthouse not found"));

        mapper.updateEntityFromWriteDto(dto, existing, userId, LocalDate.now(), courthouse);
        existing.setVersion(versionManager.increment(existing.getVersion()));

        return mapper.toReadDto(repository.save(existing));
    }

    @Override
    @Transactional
    public void delete(Long id, String userId) {
        ApplicationList list = repository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application list not found or access denied"));

        repository.delete(list);
    }
}
