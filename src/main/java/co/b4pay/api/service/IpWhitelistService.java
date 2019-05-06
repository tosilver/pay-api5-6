package co.b4pay.api.service;

import co.b4pay.api.dao.IpWhitelistDao;
import co.b4pay.api.model.IpWhitelist;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * IP白名单Service
 *
 * @author YK
 * @version $Id: IpWhitelistService.java, v 0.1 2018年4月21日 下午16:45:58 YK Exp $
 */
@Service
@Transactional
public class IpWhitelistService extends BaseService<IpWhitelistDao, IpWhitelist, Long> {
    private static final String CACHE_NAME = "IpWhitelist";

    @Cacheable(value = CACHE_NAME)
    public boolean isAccessAllowed(String merchantId, String ip) {
        IpWhitelist ipWhitelist = new IpWhitelist();
        ipWhitelist.setMerchantId(merchantId);
        ipWhitelist.setIp(ip);
        return dao.exists(Example.of(ipWhitelist));
    }
}
