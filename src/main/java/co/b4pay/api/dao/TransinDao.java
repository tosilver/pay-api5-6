package co.b4pay.api.dao;

import co.b4pay.api.model.Transin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransinDao extends JpaRepository<Transin, Long> {


    /**
     * 根据状态查询
     *
     * @param status 分账用户pid状态
     * @return 可用账户
     */
    public List<Transin> findByStatus(Integer status);
}
