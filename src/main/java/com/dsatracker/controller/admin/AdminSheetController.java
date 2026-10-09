package com.dsatracker.controller.admin;

import com.dsatracker.dto.SheetResponse;
import com.dsatracker.dto.admin.AdminSheetRequest;
import com.dsatracker.service.admin.AdminSheetService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/sheets")
@PreAuthorize("hasAuthority('SHEET_MANAGE')")
public class AdminSheetController {

    private final AdminSheetService adminSheetService;

    public AdminSheetController(AdminSheetService adminSheetService) {
        this.adminSheetService = adminSheetService;
    }

    @GetMapping
    public List<SheetResponse> list() {
        return adminSheetService.listSheets();
    }

    @PostMapping
    public SheetResponse create(@RequestBody AdminSheetRequest request) {
        return adminSheetService.createSheet(request);
    }

    @PutMapping("/{id}")
    public SheetResponse update(@PathVariable Long id, @RequestBody AdminSheetRequest request) {
        return adminSheetService.updateSheet(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        adminSheetService.deleteSheet(id);
    }
}
