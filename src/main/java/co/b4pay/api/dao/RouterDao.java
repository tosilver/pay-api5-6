package co.b4pay.api.dao;

import co.b4pay.api.model.Router;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 路由信息DAO
 *
 * @author YK
 * @version $Id: RouterDao.java, v 0.1 2018年4月23日 下午22:21:12 YK Exp $
 */
@Repository
public interface RouterDao extends JpaRepository<Router, String> {
}
