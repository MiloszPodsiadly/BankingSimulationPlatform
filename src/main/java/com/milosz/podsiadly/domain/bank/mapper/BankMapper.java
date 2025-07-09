package com.milosz.podsiadly.domain.bank.mapper;

import com.milosz.podsiadly.domain.bank.dto.BankDto;
import com.milosz.podsiadly.domain.bank.model.Bank;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring") // Wskazuje MapStructowi, aby generował komponent Springa
@Component // Opcjonalnie, ale dobra praktyka, aby IDE go rozpoznało
public interface BankMapper {

    BankDto toDto(Bank bank);
    Bank toEntity(BankDto bankDto);

    // Możesz również zdefiniować metodę aktualizującą istniejącą encję z DTO
    // @Mapping(target = "id", ignore = true)
    // void updateBankFromDto(BankDto bankDto, @MappingTarget Bank bank);
}
