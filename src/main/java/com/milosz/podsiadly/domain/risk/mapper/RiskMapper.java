package com.milosz.podsiadly.domain.risk.mapper;

import com.milosz.podsiadly.domain.risk.dto.RiskAlertDto;
import com.milosz.podsiadly.domain.risk.dto.RiskAssessmentDto;
import com.milosz.podsiadly.domain.risk.dto.RiskIndicatorDto;
import com.milosz.podsiadly.domain.risk.model.RiskAlert;
import com.milosz.podsiadly.domain.risk.model.RiskAssessment;
import com.milosz.podsiadly.domain.risk.model.RiskIndicator;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RiskMapper {

    // --- RiskIndicator Mappings ---
    RiskIndicatorDto toRiskIndicatorDto(RiskIndicator indicator);
    List<RiskIndicatorDto> toRiskIndicatorDtoList(List<RiskIndicator> indicators);
    RiskIndicator toRiskIndicatorEntity(RiskIndicatorDto indicatorDto); // For creating/updating

    // --- RiskAssessment Mappings ---
    @Mapping(source = "assessedUser.id", target = "assessedUserId")
    @Mapping(source = "assessedAccount.id", target = "assessedAccountId")
    RiskAssessmentDto toRiskAssessmentDto(RiskAssessment assessment);
    List<RiskAssessmentDto> toRiskAssessmentDtoList(List<RiskAssessment> assessments);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assessmentRef", ignore = true) // Generated in service/PrePersist
    @Mapping(target = "assessmentDate", ignore = true) // Set in service/PrePersist
    @Mapping(target = "createdAt", ignore = true) // Handled by @PrePersist
    @Mapping(target = "updatedAt", ignore = true) // Handled by @PrePersist
    @Mapping(target = "assessedUser", ignore = true) // Will be fetched/set in service based on assessedUserId
    @Mapping(target = "assessedAccount", ignore = true) // Will be fetched/set in service based on assessedAccountId
    RiskAssessment toRiskAssessmentEntity(RiskAssessmentDto assessmentDto);


    // --- RiskAlert Mappings ---
    @Mapping(source = "relatedAssessment.id", target = "relatedAssessmentId")
    RiskAlertDto toRiskAlertDto(RiskAlert alert);
    List<RiskAlertDto> toRiskAlertDtoList(List<RiskAlert> alerts);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true) // Handled by @PrePersist
    @Mapping(target = "status", ignore = true) // Default status in model/service
    @Mapping(target = "relatedAssessment", ignore = true) // Will be fetched/set in service
    RiskAlert toRiskAlertEntity(RiskAlertDto alertDto);
}