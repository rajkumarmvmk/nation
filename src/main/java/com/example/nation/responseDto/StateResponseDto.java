package com.example.nation.responseDto;
import com.example.nation.entity.StateEntity;
import com.example.nation.requestDto.StateRequestDto;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class StateResponseDto {
    private Integer stateCode;
    private String stateName;
    private CountryResponseDto countryResponseDto;
    private List<DistrictResponseDto> districtResponseDtos;

    public static StateResponseDto deserialize(StateEntity stateEntity) {
        StateResponseDto stateResponseDto= new StateResponseDto();
        stateResponseDto. setStateCode(stateEntity.getStateCode());
        stateResponseDto. setStateName(stateEntity.getStateName());
        stateResponseDto.setCountryResponseDto(CountryResponseDto.deserialize(stateEntity.getCountryEntity()));
        List<DistrictResponseDto> districtResponseDtoFinal = new ArrayList<>();
        stateEntity.getDistrictEntity().stream().forEach(districtEntity -> {
        DistrictResponseDto districtResponseDto=DistrictResponseDto.deserialize(districtEntity);
        districtResponseDtoFinal.add(districtResponseDto);
        });
         stateResponseDto.setDistrictResponseDtos(districtResponseDtoFinal);
     return stateResponseDto;
    }
}
