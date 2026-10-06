package com.smartmenu.service;

import com.smartmenu.dto.MenuLayoutPrimitiveDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @deprecated STOPPED AND REMOVED.
 * Architectural Decision: Backend is NOT a visual renderer.
 * All Java SVG DOM builders, primitive renderers, and coordinate calculations have been completely removed.
 * AI Designer now directly creates commercial F&B menu artworks via AiMenuArtworkGeneratorService.
 */
@Deprecated
@Service
@Slf4j
public class GenericSvgRendererService {

    public String renderSvg(MenuLayoutPrimitiveDto primitives) {
        log.warn("[DEPRECATED] GenericSvgRendererService is deprecated. Backend does not render menu layouts.");
        throw new UnsupportedOperationException("BACKEND_RENDERER_REMOVED: Backend no longer renders SVG layouts. Artwork is generated directly by AI Designer.");
    }
}
