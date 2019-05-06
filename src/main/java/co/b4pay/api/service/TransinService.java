package co.b4pay.api.service;

import co.b4pay.api.dao.ChannelDao;
import co.b4pay.api.dao.TradeDao;
import co.b4pay.api.dao.TransinDao;
import co.b4pay.api.model.Channel;
import co.b4pay.api.model.Transin;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TransinService extends BaseService<TransinDao, Transin, Long> {
    @Override
    public Transin getOne(Long id) {
        return dao.getOne(id);
    }

    public List<Transin> findByStatus(Integer status) {
        return dao.findByStatus(status);
    }
}
