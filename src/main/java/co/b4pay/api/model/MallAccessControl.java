package co.b4pay.api.model;

import co.b4pay.api.model.base.BaseEntity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 商城地址信息
 *
 * @author YK
 * @version $Id: Channel.java, v 0.1 2018年4月21日 下午22:09:09 YK Exp $
 */
@Entity
@Table(name = "dst_mallaccess_control")
public class MallAccessControl extends BaseEntity {



    @Id
    private Long id;                                        // 唯一标识ID
    private Long merchantId;                 //商户号
    private String merchantName;            //商户名称
    private String zfbAccess;               //支付宝通道
    private String wxAccess;                //微信通道
    private String kjAccess;                //快捷通道
    private String test;                    //预留字段

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getZfbAccess() {
        return zfbAccess;
    }

    public void setZfbAccess(String zfbAccess) {
        this.zfbAccess = zfbAccess;
    }

    public String getWxAccess() {
        return wxAccess;
    }

    public void setWxAccess(String wxAccess) {
        this.wxAccess = wxAccess;
    }

    public String getKjAccess() {
        return kjAccess;
    }

    public void setKjAccess(String kjAccess) {
        this.kjAccess = kjAccess;
    }

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }
}
