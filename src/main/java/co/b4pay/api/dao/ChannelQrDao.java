//package co.b4pay.api.dao;
//
//import co.b4pay.api.model.ChannelQr;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
///**
// * 渠道二维码信息
// * Created with IntelliJ IDEA
// * Created By AZain
// * Date: 2018-10-27
// * Time: 21:55
// */
//@Repository
//public interface ChannelQrDao extends JpaRepository<ChannelQr, Long> {
//
//    /**
//     *
//     */
//    //@Query(value = "SELECT * FROM dst_trade t WHERE t.merchant_order_no = :merchantOrderNo LIMIT 0,1",
//    //        nativeQuery = true)
//    //ChannelQr getEffectiveQrByRouter(@Param());
//
//}