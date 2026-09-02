package com.uestcfir.mapper;

import com.uestcfir.pojo.entity.ImportantNews;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImportantNewsMapper {

    // 插入重要事件
    boolean insertImportantNews(ImportantNews importantNews);

    // 查询所有重要事件
    List<ImportantNews> selectAllImportantNews();

    // 根据状态查询重要事件
    List<ImportantNews> selectImportantNewsByStatus(String status);

    // 更新重要事件
    boolean updateImportantNews(ImportantNews importantNews);

    // 删除重要事件
    boolean deleteImportantNews(Integer id);

    // 根据ID查询重要事件
    ImportantNews selectImportantNewsById(Integer id);
    //实时更新已过期列表
    int updateExpiredNewsStatus();

    //上传重要信息图片URL
    boolean uploadImportantNewsUrl(Integer id,String originalFileUrl );

    List<ImportantNews> selectNewsBySeverityAndStatus(
            @Param("severity") String severity,   // 严重程度参数
            @Param("status") String status        // 状态参数
    );
}