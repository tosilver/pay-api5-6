package co.b4pay.api.dao;

import co.b4pay.api.model.FrozenCapitalTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 冻结资金池流水DAO
 *
 * @author YK
 * @version $Id: ChannelDao.java, v 0.1 2018年4月21日 下午22:09:12 YK Exp $
 */
@Repository
public interface FrozenCapitalTradeDao extends JpaRepository<FrozenCapitalTrade, Long> {


}