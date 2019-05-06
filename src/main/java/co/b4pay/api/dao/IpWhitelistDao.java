package co.b4pay.api.dao;

import co.b4pay.api.model.IpWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * IP白名单DAO
 *
 * @author YK
 * @version $Id: IpWhitelistDao.java, v 0.1 2018年4月21日 下午16:59:12 YK Exp $
 */
@Repository
public interface IpWhitelistDao extends JpaRepository<IpWhitelist, Long> {
}
