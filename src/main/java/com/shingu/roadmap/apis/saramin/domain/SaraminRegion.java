package com.shingu.roadmap.apis.saramin.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SaraminRegion {
    @Id
    private Integer code; // ex) 10200 (서울특별시)

    @Column(nullable = false, length = 100)
    private String name; // ex) 서울특별시

    @Column
    private Integer regionCode1; // 1차 지역코드 (예: 101000)

    @Column
    private Integer regionCode2; // 2차 지역코드 (예: 101001)
}
