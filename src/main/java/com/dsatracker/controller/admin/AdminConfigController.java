package com.dsatracker.controller.admin;

import com.dsatracker.dto.admin.MetaConfigResponse;
import com.dsatracker.dto.admin.MetaConfigUpsertRequest;
import com.dsatracker.model.User;
import com.dsatracker.service.admin.AdminConfigService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/configs")
@PreAuthorize("hasAuthority('CONFIG_MANAGE')")
public class AdminConfigController {

    private final AdminConfigService adminConfigService;

    public AdminConfigController(AdminConfigService adminConfigService) {
        this.adminConfigService = adminConfigService;
    }

    @GetMapping
    public List<MetaConfigResponse> list() {
        return adminConfigService.listConfigs();
    }

    @GetMapping("/{context}")
    public MetaConfigResponse get(@PathVariable String context) {
        return adminConfigService.getConfig(context);
    }

    @PutMapping("/{context}")
    public MetaConfigResponse upsert(
            @AuthenticationPrincipal User currentAdmin,
            @PathVariable String context,
            @RequestBody MetaConfigUpsertRequest request
    ) {
        return adminConfigService.upsertConfig(context, request, currentAdmin);
    }

    @DeleteMapping("/{context}")
    public void delete(@PathVariable String context) {
        adminConfigService.deleteConfig(context);
    }
}
