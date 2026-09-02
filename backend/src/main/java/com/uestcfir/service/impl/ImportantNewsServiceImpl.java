package com.uestcfir.service.impl;

import com.uestcfir.exception.BusinessException;
import com.uestcfir.mapper.ImportantNewsMapper;
import com.uestcfir.pojo.entity.ImportantNews;
import com.uestcfir.service.ImportantNewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 重要事件管理服务实现类
 */
@Service
public class ImportantNewsServiceImpl implements ImportantNewsService {

    @Autowired
    private ImportantNewsMapper importantNewsMapper;

    @Override
    public boolean addImportantNews(ImportantNews importantNews) {
        // 基本验证
        if (importantNews.getTitle() == null || importantNews.getTitle().trim().isEmpty()) {
            throw new BusinessException("标题不能为空");
        }
        if (importantNews.getContent() == null || importantNews.getContent().trim().isEmpty()) {
            throw new BusinessException("内容不能为空");
      }
//        if (importantNews.getRelatedRoomId() == null) {
//            throw new BusinessException("关联会议室不能为空");
//        }

        // 设置默认值
        if (importantNews.getStatus() == null) {
            importantNews.setStatus("未发布");
        }
        if (importantNews.getSeverity() == null) {
            importantNews.setSeverity("中");
        }

        return importantNewsMapper.insertImportantNews(importantNews);
    }

    @Override
    public List<ImportantNews> getAllImportantNews() {
        return importantNewsMapper.selectAllImportantNews();
    }

    @Override
    public List<ImportantNews> getImportantNewsByStatus(String status) {
        return importantNewsMapper.selectImportantNewsByStatus(status);
    }

    @Override
    public boolean updateImportantNews(ImportantNews importantNews) {
        // 前端传什么就修改什么，不进行额外验证
        return importantNewsMapper.updateImportantNews(importantNews);
    }

    @Override
    public boolean deleteImportantNews(Integer id) {
        return importantNewsMapper.deleteImportantNews(id);
    }


    @Override
    public List<ImportantNews> getImportantNewsByPriority(int limit) {
        List<ImportantNews> result = new ArrayList<>();

        // 1. 先获取所有"紧急"级别的未过期事件
        List<ImportantNews> urgentNews = importantNewsMapper.selectNewsBySeverityAndStatus("紧急", "未过期");
        if (urgentNews != null) {
            result.addAll(urgentNews);
        }

        // 如果已经够了或超过limit，直接返回前limit条
        if (result.size() >= limit) {
            return result.subList(0, limit);
        }

        // 2. 补充"高"级别的未过期事件
        List<ImportantNews> highNews = importantNewsMapper.selectNewsBySeverityAndStatus("高", "未过期");
        if (highNews != null) {
            // 添加时检查是否超过limit
            for (ImportantNews news : highNews) {
                if (result.size() >= limit) {
                    break;
                }
                result.add(news);
            }
        }

        // 如果已经够了，直接返回
        if (result.size() >= limit) {
            return result;
        }

        // 3. 补充"中"级别的未过期事件
        List<ImportantNews> mediumNews = importantNewsMapper.selectNewsBySeverityAndStatus("中", "未过期");
        if (mediumNews != null) {
            for (ImportantNews news : mediumNews) {
                if (result.size() >= limit) {
                    break;
                }
                result.add(news);
            }
        }

        // 如果已经够了，直接返回
        if (result.size() >= limit) {
            return result;
        }

        // 4. 补充"低"级别的未过期事件
        List<ImportantNews> lowNews = importantNewsMapper.selectNewsBySeverityAndStatus("低", "未过期");
        if (lowNews != null) {
            for (ImportantNews news : lowNews) {
                if (result.size() >= limit) {
                    break;
                }
                result.add(news);
            }
        }

        // 返回结果，不超过limit条
        return result;
    }
}
