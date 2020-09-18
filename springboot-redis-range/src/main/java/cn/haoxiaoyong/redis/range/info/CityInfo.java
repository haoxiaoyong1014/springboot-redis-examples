package cn.haoxiaoyong.redis.range.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author haoxiaoyong
 * @date created at 下午3:37 on 2020/9/18
 * @github https://github.com/haoxiaoyong1014
 * @blog www.haoxiaoyong.cn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityInfo {

    /** 城市 */
    private String city;

    /** 经度 */
    private Double longitude;

    /** 纬度 */
    private Double latitude;
}
