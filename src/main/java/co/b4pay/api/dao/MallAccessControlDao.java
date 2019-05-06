package co.b4pay.api.dao;

import co.b4pay.api.model.MallAccessControl;
import co.b4pay.api.model.MallAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 渠道信息DAO
 *
 * @author YK
 * @version $Id: ChannelDao.java, v 0.1 2018年4月21日 下午22:09:12 YK Exp $
 */
@Repository
public interface MallAccessControlDao extends JpaRepository<MallAccessControl, Long> {

    @Query("FROM MallAccessControl t WHERE t.merchantId = :merchantId")
    MallAccessControl findByMerchantId(@Param("merchantId") Long merchantId);

}