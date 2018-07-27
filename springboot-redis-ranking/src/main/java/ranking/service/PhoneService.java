package ranking.service;
import ranking.entity.DynamicInfo;
import ranking.entity.PhoneInfo;

import java.util.List;

/**
 * Created by haoxy on 2018/7/26.
 * E-mail:hxyHelloWorld@163.com
 * github:https://github.com/haoxiaoyong1014
 */
public interface PhoneService {

    /**
     * 购买
     * @param phoneId 手机ID
     */
    void buyPhone(int phoneId);

    /**
     * 获取销量排行榜
     * @return
     */
    List<PhoneInfo> getPhbList();

    /**
     * 获得销售动态
     * @return
     */
    List<DynamicInfo> getBuyDynamic();

    /**
     * 获得销售排行榜上该手机的排名
     * @param phoneId
     * @return
     */
    int phoneRank(int phoneId);

    /**
     * 清空缓存
     */
    void clear();

    /**
     * 排行榜丢失时，根据数据库数据来初始化排行榜
     */
    void initCache();
}
