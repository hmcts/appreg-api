package uk.gov.hmcts.appregister.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.dto.read.CourtHouseDto;
import uk.gov.hmcts.appregister.mapper.CourtHouseMapper;
import uk.gov.hmcts.appregister.model.CourtHouse;
import uk.gov.hmcts.appregister.repository.CourtHouseRepository;
import uk.gov.hmcts.appregister.service.api.CourtLocationService;

@Service
@RequiredArgsConstructor
public class CourtLocationServiceImpl implements CourtLocationService {

    private final CourtHouseRepository repository;
    private final CourtHouseMapper mapper;

    @Override
    public List<CourtHouseDto> findAll() {
        final List<CourtHouse> courtHouses = repository.findAll();

        return courtHouses.stream().map(mapper::toReadDto).toList();
    }

    @Override
    public CourtHouseDto findById(Long id) {
        CourtHouse courtHouse =
                repository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Courthouse not found"));

        return mapper.toReadDto(courtHouse);
    }
}
