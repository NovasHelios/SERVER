package com.heilous.vworld.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VWorldLandRequest {

    @NotBlank(message = "고유번호(PNU)는 필수입니다")
    private String pnu;

    private String format = "json"; // xml 또는 json
    private Integer numOfRows = 10; // 최대 1000
    private Integer pageNo = 1;
}
