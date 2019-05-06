package co.b4pay.api.service;

import co.b4pay.api.dao.RouterDao;
import co.b4pay.api.model.Router;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 路由信息Service
 *
 * @author YK
 * @version $Id: RouterService.java, v 0.1 2018年4月23日 下午22:28:58 YK Exp $
 */
@Service
@Transactional
public class RouterService extends BaseService<RouterDao, Router, String> {

}
