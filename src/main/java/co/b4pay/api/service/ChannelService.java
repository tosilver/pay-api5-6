package co.b4pay.api.service;

import co.b4pay.api.dao.ChannelDao;
import co.b4pay.api.model.Channel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChannelService extends BaseService<ChannelDao, Channel, Long> {

    public Channel getChannel(String channelName) {
        return dao.findByName(channelName);
    }

}
