package com.dsatracker.controller;

import com.dsatracker.dto.SheetResponse;
import com.dsatracker.service.SheetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sheets")
public class SheetController {

    private final SheetService sheetService;

    public SheetController(SheetService sheetService) {
        this.sheetService = sheetService;
    }

    @GetMapping
    public List<SheetResponse> getSheets() {
        return sheetService.getAllSheets();
    }
}
