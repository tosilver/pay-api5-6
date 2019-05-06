package co.b4pay.api.dao;

import co.b4pay.api.common.enums.ChannelType;
import co.b4pay.api.model.JobTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface JobTradeDao extends JpaRepository<JobTrade, String> {

    @Query("from JobTrade t where t.channelType = :channelType AND t.status = :status AND t.execTime <= :execTime")
    List<JobTrade> findByChannelTypeAndStatusAndExecTimeLessThanEquals(@Param("channelType") ChannelType channelType, @Param("status") Integer status, @Param("execTime") Date execTime);
}
