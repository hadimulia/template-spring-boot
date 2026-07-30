package com.template.service.export;

import com.template.dto.user.UserResponse;
import com.template.mapper.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private final UserMapper userMapper;

    public byte[] exportUsersToExcel(String keyword) throws IOException {
        List<UserResponse> users = userMapper.findAll(keyword, 0, Integer.MAX_VALUE);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Users");

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setFont(createBoldFont(workbook));

            String[] headers = {"No", "Username", "Full Name", "Email", "Status"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < users.size(); i++) {
                Row row = sheet.createRow(i + 1);
                UserResponse user = users.get(i);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(user.getUsername());
                row.createCell(2).setCellValue(user.getFullname());
                row.createCell(3).setCellValue(user.getEmail());
                row.createCell(4).setCellValue(Boolean.TRUE.equals(user.getEnabled()) ? "Active" : "Inactive");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    private Font createBoldFont(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        return font;
    }
}
