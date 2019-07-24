package co.b4pay.api.dao;

import co.b4pay.api.model.DisabledTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisabledTradeDao extends JpaRepository<DisabledTrade, Long> {


}
