package co.b4pay.api.service;

import co.b4pay.api.dao.MerchantDao;
import co.b4pay.api.model.Merchant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MerchantService extends BaseService<MerchantDao, Merchant, Long> {

}
