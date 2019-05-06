package co.b4pay.api.service;

import co.b4pay.api.dao.ChannelDao;
import co.b4pay.api.dao.MallAddressDao;
import co.b4pay.api.model.Channel;
import co.b4pay.api.model.MallAddress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MallAddressService extends BaseService<MallAddressDao, MallAddress, Long> {


}
