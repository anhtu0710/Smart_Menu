package com.smartmenu.controller;

import com.smartmenu.dto.ApiResponse;
import com.smartmenu.dto.FullAnalysisResponseData;

import com.smartmenu.service.MenuGenerationService;
import com.smartmenu.service.SessionAccessService;
import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/smartcoffee")
@RequiredArgsConstructor
public class AnalysisController {

    private final MenuGenerationService menuGenerationService;
    private final SessionAccessService sessionAccessService;

    /**
     *
     * Bắt đầu phân tích
     *
     * FE truyền sessionId
     *
     */
    @PostMapping("/analyze-full/{sessionId}")
    public ResponseEntity<ApiResponse<FullAnalysisResponseData>> analyzeFull(

            @PathVariable Long sessionId,
            HttpServletRequest request

    ) {

        sessionAccessService.requireSessionOwner(sessionId, request);
        FullAnalysisResponseData result =

                menuGenerationService

                        .analyzeFullWorkflow(

                                sessionId

                        );

        return ResponseEntity.ok(

                ApiResponse.success(

                        result,

                        "Phân tích SmartMenu hoàn tất"

                )

        );

    }

    /**
     *
     * Lấy lại kết quả phân tích
     *
     */
    @GetMapping("/analysis/{sessionId}")
    public ResponseEntity<ApiResponse<FullAnalysisResponseData>> getAnalysis(

            @PathVariable Long sessionId,
            HttpServletRequest request

    ) {

        sessionAccessService.requireSessionOwner(sessionId, request);
        FullAnalysisResponseData result =

                menuGenerationService

                        .getAnalysisBySessionId(

                                String.valueOf(
                                        sessionId)

                        );

        return ResponseEntity.ok(

                ApiResponse.success(

                        result,

                        "Lấy dữ liệu thành công"

                )

        );

    }

}
