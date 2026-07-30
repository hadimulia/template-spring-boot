package com.template.mapper.approval;

import com.template.dto.approval.ApprovalRequestResponse;
import com.template.entity.approval.ApprovalRequest;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface ApprovalRequestMapper extends Mapper<ApprovalRequest> {

    List<ApprovalRequestResponse> findPage(@Param("keyword") String keyword,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    int countPage(@Param("keyword") String keyword);

    List<ApprovalRequestResponse> findPending();
}
