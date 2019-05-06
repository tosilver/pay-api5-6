package co.b4pay.api.service;

import co.b4pay.api.dao.JobTradeDao;
import co.b4pay.api.model.JobTrade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class JobTradeService extends BaseService<JobTradeDao, JobTrade, String> {

}
