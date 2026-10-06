package com.smartmenu.service;

import com.smartmenu.dto.ProductImageRule;
import com.smartmenu.dto.MenuLayoutPrimitiveDto;
import org.springframework.stereotype.Service;

@Service
public class ProductImageResolver {

    /**
     * Resolves product image/graphic primitive based on productId and ProductImageRule.
     */
    public MenuLayoutPrimitiveDto.HeroPrimitive resolveHeroImagePrimitive(
            String productId, 
            String categoryId, 
            ProductImageRule imageRule,
            int x, int y, int width, int height) {
        
        String placement = (imageRule != null && imageRule.getImagePlacement() != null) 
                ? imageRule.getImagePlacement() : "LEFT";
        String masking = (imageRule != null && imageRule.getImageMasking() != null) 
                ? imageRule.getImageMasking() : "ROUND_RECT";
        String shadow = (imageRule != null && imageRule.getShadowStyle() != null) 
                ? imageRule.getShadowStyle() : "SOFT_DROP";

        MenuLayoutPrimitiveDto.HeroImagePrimitive imgPrim = MenuLayoutPrimitiveDto.HeroImagePrimitive.builder()
                .productId(productId)
                .foodImageUrl("graphic://dish-placeholder-" + productId)
                .x(x)
                .y(y)
                .width(width)
                .height(height)
                .imagePlacement(placement)
                .imageShape(masking)
                .shadowStyle(shadow)
                .lightingStyle("HIGH_CONTRAST_LUXURY")
                .build();

        return MenuLayoutPrimitiveDto.HeroPrimitive.builder()
                .productId(productId)
                .categoryId(categoryId)
                .displayGroup("HERO")
                .displaySize("LARGE")
                .displayPosition("TOP")
                .placementGroup("FEATURED")
                .displayPriority(1)
                .x(x)
                .y(y)
                .width(width)
                .height(height)
                .imagePrimitive(imgPrim)
                .heroBgFill("#1e293b")
                .heroStroke("#f59e0b")
                .build();
    }
}
