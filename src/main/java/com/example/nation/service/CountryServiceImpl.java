package com.example.nation.service;

import com.example.nation.dao.CountryDao;
import com.example.nation.dao.DistrictDao;
import com.example.nation.dao.StateDao;
import com.example.nation.entity.CountryEntity;
import com.example.nation.entity.DistrictEntity;
import com.example.nation.entity.StateEntity;
import com.example.nation.exception.customException.BaseErrorCodes;
import com.example.nation.exception.customException.BusinessException;


import com.example.nation.exception.customException.TechnicalException;
import com.example.nation.requestDto.CountryRequestDto;
import com.example.nation.requestDto.CountryUpdateRequestDto;
import com.example.nation.responseDto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;


@Slf4j
@RequiredArgsConstructor
@Component
public class CountryServiceImpl implements CountryService {

    private final CountryDao countryDao;
    private final StateDao stateDao;
    private final StateService stateService;

    @Override
    public CountryResponseDto register(CountryRequestDto countryRequestDto) throws BusinessException, TechnicalException {
        Optional<CountryEntity> countryEntityCheck =countryDao.isCodeExists(countryRequestDto.getCountryCode());
        if(countryEntityCheck.isEmpty()){
            CountryEntity countryEntity  =countryRequestDto.serialize();
            List<StateGetAndUpResponseDto> stateGetAndUpResponseDtos = new ArrayList<>();
           try{
              countryDao.save(countryEntity);
               List<StateEntity> stateEntities=new ArrayList<>();
               countryRequestDto.getStateForCounRequestDtos().stream().forEach(state->{
                   StateEntity stateEntity =state.serialize();
                   stateEntity.setCountryEntity(countryEntity);
                   stateDao.save(stateEntity);
                   log.debug("-----------------{}------------------------",stateEntity);
                   stateEntities.add(stateEntity);
                   StateGetAndUpResponseDto stateGetAndUpResponseDto= StateGetAndUpResponseDto.deserialize(stateEntity);
                   stateGetAndUpResponseDtos.add(stateGetAndUpResponseDto);
               });

           }catch (Exception e)
           {
               throw new BusinessException(BaseErrorCodes.TECHNICAL_EXCEPTION.name(),BaseErrorCodes.TECHNICAL_EXCEPTION.message());
           }
            CountryResponseDto countryResponseDtoFinal=CountryResponseDto.deserialize(countryEntity);
            countryResponseDtoFinal.setStateGetAndUpResponseDtos(stateGetAndUpResponseDtos);
            return countryResponseDtoFinal;

        }
        else {
            throw new BusinessException(BaseErrorCodes.DUPLICATE_RECORD.message());

        }

        }
        @Override
        @Cacheable("getAllList")
        public List<CountryAllRecord> getAll() {
            List<CountryEntity> countryEntity = countryDao.getAll();
            log.info("-----------------{}------------------------",countryEntity.size());
            List<CountryAllRecord> countryAllRecords = new ArrayList<>();
            countryEntity.forEach(obj -> {
                CountryAllRecord countryAllRecord = CountryAllRecord.deserialize(obj);
                List<StateResponseGetDto> stateResponseGetDtos = new ArrayList<>();
                obj.getStateEntity().stream().forEach(state -> {
                    StateResponseGetDto stateResponseGetDto = StateResponseGetDto.deserialize(state);
                    log.info("-----------------{}------------------------",stateResponseGetDto);
                    stateResponseGetDtos.add(stateResponseGetDto);
                });
                countryAllRecord.setStateResponseGetDtos(stateResponseGetDtos);
                countryAllRecords.add(countryAllRecord);
            });
            return countryAllRecords;
        }
        @Override
        public CountryGetAndUpResponseDto update(String countryCode, CountryUpdateRequestDto countryUpdateRequestDto) throws BusinessException, TechnicalException {
        Optional<CountryEntity> countryEntity=countryDao.isCodeExists(countryCode);
            if(countryEntity.isPresent()){
                        countryEntity.get().setCountryName(countryUpdateRequestDto.getCountryName());
                        countryEntity.get().setCapital(countryUpdateRequestDto.getCapital());
                        countryEntity.get().setContinent(countryUpdateRequestDto.getContinent());
            try {
                countryDao.save(countryEntity.get());
            }catch (Exception e) {
                throw new TechnicalException(BaseErrorCodes.TECHNICAL_EXCEPTION.name(),BaseErrorCodes.TECHNICAL_EXCEPTION.message());
            }

            return  CountryGetAndUpResponseDto.deserialize(countryEntity.get());

        }else{
            throw new BusinessException(BaseErrorCodes.RECORD_NOT_FOUND.name(),BaseErrorCodes.RECORD_NOT_FOUND.message());
        }

        }
    @Override
    @Transactional
    public String delete(String countryCode) throws BusinessException {
        Optional<CountryEntity> countryEntity = countryDao.isCodeExists(countryCode);
        if(countryEntity.isPresent()){
            stateService.deleteByCountryId(countryEntity.get().getId());
            countryDao.delete(countryEntity.get().getId());
            return "deleted successfully";
        }else{
            throw new BusinessException(BaseErrorCodes.RECORD_NOT_FOUND.name(),BaseErrorCodes.RECORD_NOT_FOUND.message());
        }
    }

    }

