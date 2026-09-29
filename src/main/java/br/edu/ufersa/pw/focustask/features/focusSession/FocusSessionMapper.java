package br.edu.ufersa.pw.focustask.features.focusSession;

import br.edu.ufersa.pw.focustask.features.focusSession.dto.FocusSessionResponseDTO;
import br.edu.ufersa.pw.focustask.features.focusSession.dto.FocusSessionStatusDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface FocusSessionMapper {
    FocusSessionResponseDTO toResponse(FocusSession entity);
    List<FocusSessionResponseDTO> toResponseList(List<FocusSession> entities);
    FocusSessionStatusDTO toStatusDto(FocusSessionStatus status);
}
