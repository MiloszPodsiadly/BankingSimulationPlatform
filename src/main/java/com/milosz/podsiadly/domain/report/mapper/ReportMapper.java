package com.milosz.podsiadly.domain.report.mapper;

import com.milosz.podsiadly.domain.report.model.BankStatement;
import com.milosz.podsiadly.domain.report.dto.BalanceSheetDto; // Jeśli będziemy mapować BankStatement do DTO, przyda się
import com.milosz.podsiadly.domain.report.dto.FinancialSummaryDto;
import com.milosz.podsiadly.domain.report.dto.ProfitAndLossStatementDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReportMapper {

    // Example: Mapping for BankStatement to a potential DTO (if we define BankStatementDto later)
    // For now, let's assume we might need it, or for mapping a request object if any.

    // A simple method to illustrate mapping an entity to a DTO if needed for BankStatement
    // If you plan to expose BankStatement entities via API, you'd create a BankStatementDto record.
    /*
    @Mapping(target = "id", source = "id")
    BankStatementDto toBankStatementDto(BankStatement bankStatement);
    List<BankStatementDto> toBankStatementDtoList(List<BankStatement> bankStatements);
    */

    // As for the other DTOs like BalanceSheetDto, ProfitAndLossStatementDto,
    // they are often *built* directly in the service layer from aggregated data,
    // rather than mapped directly from a single entity.
    // However, if parts of them come from an entity, a mapping would go here.

    // For now, no direct entity-to-DTO mappings for the complex report DTOs like BalanceSheetDto etc.,
    // as their creation logic will be in the service layer (DataAggregator/ReportGenerator).
    // This mapper will be more relevant if we introduce intermediate entities or
    // if a complex DTO is derived from a single, complex entity.
}