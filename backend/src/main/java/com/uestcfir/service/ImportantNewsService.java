package com.uestcfir.service;

import com.uestcfir.pojo.entity.ImportantNews;
import java.util.List;

/**
 * 重要事件管理服务接口
 */
public interface ImportantNewsService {

    /**
     * 添加重要事件
     */
    boolean addImportantNews(ImportantNews importantNews);

    /**
     * 获取所有重要事件
     */
    List<ImportantNews> getAllImportantNews();

    /**
     * 根据状态获取重要事件
     */
    List<ImportantNews> getImportantNewsByStatus(String status);

    /**
     * 更新重要事件
     */
    boolean updateImportantNews(ImportantNews importantNews);

    /**
     * 删除重要事件
     */
    boolean deleteImportantNews(Integer id);

    /**
     * 按优先级获取重要事件（最多8条）
     * 优先获取紧急程度高的，按紧急→高→中→低的顺序补充
     */
    List<ImportantNews> getImportantNewsByPriority(int limit);
}