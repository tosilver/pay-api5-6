package co.b4pay.api.dao;

import co.b4pay.api.model.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 渠道信息DAO
 *
 * @author YK
 * @version $Id: ChannelDao.java, v 0.1 2018年4月21日 下午22:09:12 YK Exp $
 */
@Repository
public interface ChannelDao extends JpaRepository<Channel, Long> {

    @Query("FROM Channel t WHERE t.router.id = :routerId AND t.status = :status AND t.ip4 = :ip4 ORDER BY t.updateTime,t.amountLimit desc")
    List<Channel> findByRouterIdAndStatus(@Param("routerId") String routerId,
                                          @Param("status") int status,
                                          @Param("ip4") String ip4);

    @Query("FROM Channel t WHERE t.router.id = :routerId AND t.status = :status ORDER BY t.updateTime,t.amountLimit desc")
    List<Channel> findByRouterIdAndStatus(@Param("routerId") String routerId,
                                          @Param("status") int status);

    @Query("FROM Channel t WHERE t.name =:channelName")
    Channel findByName(@Param("channelName") String channelName);

    @Query("FROM Channel t WHERE t.router.id = :routerId and t.name =:channelName")
    Channel findByRouterAndName(@Param("routerId") String routerId, @Param("channelName") String channelName);

//    @Query("FROM Channel t WHERE t.resetTime <= :now")
//    List<Channel> findByResetTimeLessThanEquals(Date now);
}