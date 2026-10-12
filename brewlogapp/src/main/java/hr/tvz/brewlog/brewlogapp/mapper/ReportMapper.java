package hr.tvz.brewlog.brewlogapp.mapper;

import hr.tvz.brewlog.brewlogapp.dto.AdminReportResponse;
import hr.tvz.brewlog.brewlogapp.dto.ReportResponse;
import hr.tvz.brewlog.brewlogapp.model.AggregateReport;
import hr.tvz.brewlog.brewlogapp.model.Report;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReportMapper {

    ReportResponse toResponse(Report report);

    AdminReportResponse toAdminResponse(AggregateReport report);
}
