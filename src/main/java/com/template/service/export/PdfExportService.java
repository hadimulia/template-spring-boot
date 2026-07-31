package com.template.service.export;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import com.template.dto.user.UserResponse;
import com.template.mapper.user.UserMapper;
import com.template.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final UserMapper userMapper;

    public byte[] exportUsersToPdf(String keyword) throws DocumentException, IOException {
        List<UserResponse> users = userMapper.findAll(keyword, TenantContext.getTenantId(), 0, Integer.MAX_VALUE);

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, bos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("User Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            headerFont.setColor(Color.WHITE);
            PdfPCell headerCell;

            String[] headers = {"No", "Username", "Full Name", "Email", "Status"};
            for (String h : headers) {
                headerCell = new PdfPCell(new Phrase(h, headerFont));
                headerCell.setBackgroundColor(new Color(0, 102, 204));
                headerCell.setPadding(8);
                headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(headerCell);
            }

            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            for (int i = 0; i < users.size(); i++) {
                UserResponse user = users.get(i);
                table.addCell(new PdfPCell(new Phrase(String.valueOf(i + 1), cellFont)));
                table.addCell(new PdfPCell(new Phrase(user.getUsername(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(user.getFullname(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(user.getEmail(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(
                        Boolean.TRUE.equals(user.getEnabled()) ? "Active" : "Inactive", cellFont)));
            }

            document.add(table);
            document.close();
            return bos.toByteArray();
        }
    }
}
