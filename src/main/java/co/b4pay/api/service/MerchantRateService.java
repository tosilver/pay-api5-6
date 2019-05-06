package co.b4pay.api.service;

import co.b4pay.api.dao.MerchantRateDao;
import co.b4pay.api.model.MerchantRate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MerchantRateService extends BaseService<MerchantRateDao, MerchantRate, Long> {

}
