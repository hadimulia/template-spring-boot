package com.template.service.export;

import com.template.entity.user.User;
import com.template.entity.user.UserRole;
import com.template.mapper.user.UserMapper;
import com.template.mapper.user.UserRoleMapper;
import com.template.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public List<String> importUsers(MultipartFile file) throws IOException {
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String username = getCellValue(row.getCell(0));
                String fullname = getCellValue(row.getCell(1));
                String email = getCellValue(row.getCell(2));
                String password = getCellValue(row.getCell(3));

                if (username.isBlank()) {
                    errors.add("Row " + (i + 1) + ": username is required");
                    continue;
                }

                if (userMapper.findByUsername(username) != null) {
                    errors.add("Row " + (i + 1) + ": username '" + username + "' already exists");
                    continue;
                }

                User user = new User();
                user.setUsername(username);
                user.setFullname(fullname.isBlank() ? username : fullname);
                user.setEmail(email.isBlank() ? null : email);
                user.setPassword(passwordEncoder.encode(password.isBlank() ? "123456" : password));
                user.setEnabled(true);
                user.setAccountLocked(false);
                user.setLoginAttempts(0);
                user.setCreatedBy(SecurityUtils.getCurrentUsername());
                user.setCreatedDate(LocalDateTime.now());
                user.setDeleted(false);
                user.setVersion(0);
                userMapper.insert(user);
                rowCount++;
            }

            if (rowCount > 0 && errors.isEmpty()) {
                errors.add("Successfully imported " + rowCount + " users");
            } else if (rowCount > 0) {
                errors.add(0, "Imported " + rowCount + " users with some errors:");
            } else {
                errors.add("No users were imported");
            }
        }

        return errors;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
