package com.dsatracker.service;

import com.dsatracker.dto.SheetResponse;
import com.dsatracker.repository.SheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SheetService {

    private final SheetRepository sheetRepository;

    public SheetService(SheetRepository sheetRepository) {
        this.sheetRepository = sheetRepository;
    }

    @Transactional(readOnly = true)
    public List<SheetResponse> getAllSheets() {
        return sheetRepository.findAllByOrderByOrderIndexAsc().stream()
                .map(SheetResponse::from)
                .toList();
    }
}
