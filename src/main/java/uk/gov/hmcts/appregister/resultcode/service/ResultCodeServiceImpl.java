package uk.gov.hmcts.appregister.resultcode.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.resultcode.dto.ResultCodeDto;
import uk.gov.hmcts.appregister.resultcode.mapper.ResultCodeMapper;
import uk.gov.hmcts.appregister.resultcode.model.ResultCode;
import uk.gov.hmcts.appregister.resultcode.repository.ResultCodeRepository;

@Service
@RequiredArgsConstructor
public class ResultCodeServiceImpl implements ResultCodeService {

    private final ResultCodeRepository repository;
    private final ResultCodeMapper mapper;

    @Override
    public List<ResultCodeDto> findAll() {
        final List<ResultCode> resultCodes = repository.findAll();

        return resultCodes.stream().map(mapper::toReadDto).toList();
    }

    @Override
    public ResultCodeDto findByCode(String code) {

        final ResultCode resultCode =
                repository
                        .findByResultCode(code)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "ResultCode not found"));

        return mapper.toReadDto(resultCode);
    }
}
