package uk.gov.hmcts.appregister.applicationlist.service;

import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.applicationlist.dto.ApplicationListDto;
import uk.gov.hmcts.appregister.applicationlist.dto.ApplicationListWriteDto;
import uk.gov.hmcts.appregister.applicationlist.mapper.ApplicationListMapper;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
import uk.gov.hmcts.appregister.common.security.UserProvider;

/** Service implementation for managing application lists. */
@RequiredArgsConstructor
@Service
public class ApplicationListServiceImpl implements ApplicationListService {

    private final ApplicationListRepository repository;
    private final ApplicationListMapper mapper;
    private final NationalCourtHouseRepository courtHouseRepository;
    private final UserProvider appRegUser;

    @Override
    public List<ApplicationListDto> getAll() {
        return repository.findAll().stream().map(mapper::toReadDto).toList();
    }

    @Override
    public ApplicationListDto getByIdForUser(Long id) {
        return repository
                .findByIdAndCreatedUser(id, appRegUser.getUser())
                .map(mapper::toReadDto)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Application list not found"));
    }

    @Override
    @Transactional
    public ApplicationListDto create(ApplicationListWriteDto dto) {
        NationalCourtHouse courthouse =
                courtHouseRepository
                        .findById(dto.courthouseId())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST, "Courthouse not found"));

        ApplicationList entity = mapper.createEntityFromWriteDto(dto, courthouse);

        return mapper.toReadDto(repository.save(entity));
    }

    @Override
    @Transactional
    public ApplicationListDto update(Long id, ApplicationListWriteDto dto) {
        ApplicationList existing =
                repository
                        .findByIdAndCreatedUser(id, appRegUser.getUser())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Application list not found"));

        NationalCourtHouse courthouse =
                courtHouseRepository
                        .findById(dto.courthouseId())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST, "Courthouse not found"));

        mapper.updateEntityFromWriteDto(dto, existing, courthouse);

        return mapper.toReadDto(repository.save(existing));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ApplicationList list =
                repository
                        .findByIdAndCreatedUser(id, appRegUser.getUser())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Application list not found or access denied"));

        repository.delete(list);
    }
}
