package uk.gov.hmcts.appregister.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.dto.read.ResultCodeDto;
import uk.gov.hmcts.appregister.mapper.ResultCodeMapper;
import uk.gov.hmcts.appregister.model.ResultCode;
import uk.gov.hmcts.appregister.repository.ResultCodeRepository;
import uk.gov.hmcts.appregister.service.api.ResultCodeService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResultCodeServiceImpl implements ResultCodeService {

    private final ResultCodeRepository repository;
    private final ResultCodeMapper mapper;

    @Override
    public List<ResultCodeDto> findAll() {
        final List<ResultCode> resultCodes = repository.findAll();

        return resultCodes.stream()
            .map(mapper::toReadDto)
            .toList();
    }

    @Override
    public ResultCodeDto findByCode(String code) {

        final ResultCode resultCode = repository.findByResultCode(code)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ResultCode not found"));

        return mapper.toReadDto(resultCode);

    }
}
