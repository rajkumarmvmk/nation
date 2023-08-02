package com.example.nation.service;

import com.example.nation.dao.CountryDao;
import com.example.nation.dao.DistrictDao;
import com.example.nation.dao.StateDao;
import com.example.nation.entity.CountryEntity;
import com.example.nation.entity.DistrictEntity;
import com.example.nation.entity.StateEntity;
import com.example.nation.exception.customException.BaseErrorCodes;
import com.example.nation.exception.customException.BaseException;
import com.example.nation.exception.customException.BusinessException;
import com.example.nation.repository.StateRepository;
import com.example.nation.requestDto.StateRequestDto;
import com.example.nation.requestDto.StateUpdateRequestDto;
import com.example.nation.responseDto.StateGetAndUpResponseDto;
import com.example.nation.responseDto.StateResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;


@Slf4j
@RequiredArgsConstructor
@Component
public class StateServiceImpl implements StateService {
    private final StateDao stateDao;
    private final DistrictDao districtDao;
    private final CountryDao countryDao;
    private final StateRepository stateRepository;


    @Override
    public StateResponseDto register(String countryCode, StateRequestDto stateRequestDto) throws BusinessException {
        Optional<StateEntity> stateEntity = stateDao.isStateCodeExists(stateRequestDto.getStateCode());
        if (stateEntity.isEmpty()) {
            StateEntity stateEntityFinal = stateRequestDto.serialize();
            Optional<CountryEntity> countryEntity = countryDao.isCodeExists(countryCode);
            if (countryEntity.isPresent()) {
                stateEntityFinal.setCountryEntity(countryEntity.get());
            } else {
                throw new BusinessException(BaseErrorCodes.RECORD_NOT_FOUND.name(), BaseErrorCodes.RECORD_NOT_FOUND.message());
            }
            List<DistrictEntity> districtEntityList = new ArrayList<>();
            try {
                log.info("-----------------{}------------------------",stateEntityFinal);
                stateDao.save(stateEntityFinal);
                stateRequestDto.getDistrictRequestDtos().forEach(districtRequestDto -> {
                    DistrictEntity districtEntity = districtRequestDto.serialize();
                    districtEntity.setStateEntity(stateEntityFinal);
                    districtEntityList.add(districtEntity);
                });
                districtDao.saveAll(districtEntityList);
                stateEntityFinal.setDistrictEntity(districtEntityList);
            } catch (Exception e) {
                throw new BusinessException(BaseErrorCodes.TECHNICAL_EXCEPTION.name(), BaseErrorCodes.TECHNICAL_EXCEPTION.message());
            }
            return StateResponseDto.deserialize(stateEntityFinal);
        } else {
            throw new BusinessException(BaseErrorCodes.DUPLICATE_RECORD.name(), BaseErrorCodes.DUPLICATE_RECORD.message());
        }
    }

    @Override
    public StateGetAndUpResponseDto update(Integer stateCode, StateUpdateRequestDto stateUpdateRequestDto) throws BusinessException {
        Optional<StateEntity> stateEntity = stateDao.isStateCodeExists(stateCode);
        if (stateEntity.isPresent()) {
            stateEntity.get().setStateName(stateUpdateRequestDto.getStateName());
            try {
                stateDao.save(stateEntity.get());
            } catch (Exception e) {
                throw new BusinessException(BaseErrorCodes.TECHNICAL_EXCEPTION.name(), BaseErrorCodes.TECHNICAL_EXCEPTION.message());
            }
            return StateGetAndUpResponseDto.deserialize(stateEntity.get());
        } else {
            throw new BusinessException(BaseErrorCodes.RECORD_NOT_FOUND.name(), BaseErrorCodes.RECORD_NOT_FOUND.message());
        }

    }

    @Override
    public List<StateResponseDto> getAll(Pageable pageable) {
        Page<StateEntity> stateEntities = stateDao.getAll(pageable);
        List<StateResponseDto> stateResponseDtos = new ArrayList<>();
        stateEntities.stream().forEach(obj -> {
            StateResponseDto stateResponseDto = StateResponseDto.deserialize(obj);
            stateResponseDtos.add(stateResponseDto);
        });
        return stateResponseDtos;
    }

    @Override
    @Transactional
    public String deleteByCountryId(String id) {
        log.info("-----------------{}------------------------",id);
        districtDao.deleteByStateId(stateDao.getAllStateIdByCountryId(id));
        return stateDao.deleteByCountryId(id);

    }

    @Override
    @Transactional
    public String delete(Integer stateCode) throws BusinessException {

        Optional<StateEntity> stateEntity = stateDao.isStateCodeExists(stateCode);
       if(stateEntity.isEmpty()){
        String id =  stateEntity.get().getId();
        districtDao.deleteByStateId(Collections.singletonList(id));
        stateRepository.deleteAllByStateCode(stateCode);

        return "success";
    }
       else{
           throw new BusinessException(BaseErrorCodes.RECORD_NOT_FOUND.name(),BaseErrorCodes.RECORD_NOT_FOUND.message());
           }
       }


}
